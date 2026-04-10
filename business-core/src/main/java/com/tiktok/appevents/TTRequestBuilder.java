/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.JSON;
import com.tiktok.util.SystemInfoUtil;
import com.tiktok.util.TTUtil;
import com.tiktok.util.TimeUtil;

import org.json.JSONObject;

import java.util.Date;

class TTRequestBuilder {
    private static final String TAG = "TTRequestBuilder";

    private static JSONObject basePayloadCache = null;
    private static JSONObject healthBasePayloadCache = null;
    private static boolean containTestCode = false;

    public static JSONObject getBasePayloadWithTs() {
        JSONObject basePayload = getBasePayload();
        JSON.putObject(basePayload, "timestamp", TimeUtil.getISO8601Timestamp(new Date()));
        return basePayload;
    }

    public static JSONObject getBasePayload() {
        boolean isDebugMode = TikTokBusinessSdk.isInSdkDebugMode() || TikTokBusinessSdk.isEnableDebugMode();

        try {
            if (basePayloadCache != null) {
                if (isDebugMode != containTestCode) {
                    if (isDebugMode) {
                        JSON.putObject(basePayloadCache, "test_event_code", String.valueOf(TikTokBusinessSdk.getTTAppId()));
                        containTestCode = true;
                    } else {
                        basePayloadCache.remove("test_event_code");
                        containTestCode = false;
                    }
                }
                return basePayloadCache;
            }


            JSONObject result = JSON.build();
            if (TikTokBusinessSdk.onlyAppIdProvided()) {
                // to be compatible with the old versions
                JSON.putObject(result, "app_id", TikTokBusinessSdk.getAppId());
            } else {
                JSON.putObject(result, "tiktok_app_id", TikTokBusinessSdk.getFirstTTAppIds());
            }
            if (isDebugMode) {
                JSON.putObject(result, "test_event_code", String.valueOf(TikTokBusinessSdk.getTTAppId()));
                containTestCode = true;
            }
            JSON.putObject(result, "event_source", "APP_EVENTS_SDK");

            basePayloadCache = result;
        } catch (Throwable ignore) {
        }

        if (basePayloadCache == null) {
            basePayloadCache = JSON.build();
        }

        return basePayloadCache;
    }

    private static JSONObject contextForApiCache = null;

    // the context part that does not change
    private static JSONObject getImmutableContextForApi(TTAppEvent event) {
        if (contextForApiCache != null) {
            freshOsVersion(contextForApiCache, event);
            return contextForApiCache;
        }

        TTIdentifierFactory.AdIdInfo adIdInfo = null;
        long initTimeMS = System.currentTimeMillis();
        try {
            TikTokBusinessSdk.getAppEventLogger().monitorMetric("did_start", TTUtil.getMetaWithTS(initTimeMS), null);
            if (TikTokBusinessSdk.isGaidCollectionEnabled()) {
                // fetch gaid info through google service
                adIdInfo = TTIdentifierFactory.getGoogleAdIdInfo(TikTokBusinessSdk.getApplicationContext());
            }
            long endTimeMS = System.currentTimeMillis();
            JSONObject meta = TTUtil.getMetaWithTS(endTimeMS);
            JSON.putLong(meta, "latency", endTimeMS - initTimeMS);
            JSON.putBoolean(meta, "success", adIdInfo != null && !TextUtils.isEmpty(adIdInfo.getAdId()));
            TikTokBusinessSdk.getAppEventLogger().monitorMetric("did_end", meta, null);
        } catch (Throwable ignored) {
        }

        contextForApiCache = contextBuilderWithLocalAndLibrary(adIdInfo);
        freshOsVersion(contextForApiCache, event);
        return contextForApiCache;
    }

    public static JSONObject ddlJson() {
        try {
            JSONObject requestBody = JSON.build();
            TTIdentifierFactory.AdIdInfo adIdInfo = null;
            if (TikTokBusinessSdk.isGaidCollectionEnabled()) {
                adIdInfo = TTIdentifierFactory.getGoogleAdIdInfo(TikTokBusinessSdk.getApplicationContext());
            }

            JSONObject jsonObject = contextBuilder(adIdInfo, true);
            JSON.putObject(jsonObject, "user", TTUserInfo.sharedInstance.toJsonObject());

            JSON.putObject(requestBody, "tiktok_app_id", TikTokBusinessSdk.getTTAppId());
            JSON.putObject(requestBody, "context", jsonObject);
            JSON.putObject(requestBody, "timestamp", TimeUtil.getISO8601Timestamp(new Date(System.currentTimeMillis())));
            JSON.putObject(requestBody, "ip", SystemInfoUtil.getLocalIpAddress());
            String userAgent = SystemInfoUtil.getUserAgent();
            if (userAgent != null) {
                JSON.putObject(requestBody, "user_agent", userAgent);
            }
            return requestBody;
        } catch (Throwable ignore) {
            return JSON.build();
        }
    }

    private static void freshOsVersion(JSONObject contextForApiCache, TTAppEvent event) {
        try {
            JSONObject device = JSON.getJsonObject(contextForApiCache, "device");
            if (event != null && device != null) {
                JSON.putObject(device, "os_version", SystemInfoUtil.getAndroidVersion());

                //remove
                JSON.putObject(device, "version", null);
            } else {
                JSON.putObject(device, "version", SystemInfoUtil.getAndroidVersion());

                //remove
                JSON.putObject(device, "os_version", null);
            }
        } catch (Throwable ignore) {
        }
    }

    public static JSONObject getContextForApi(TTAppEvent event) {
        try {
            JSONObject immutablePart = getImmutableContextForApi(event);
            JSONObject finalObj = JSON.build(immutablePart.toString());
            JSON.putObject(finalObj, "user", event.getUserInfo().toJsonObject());
            return finalObj;
        } catch (Throwable ignore) {
            return JSON.build();
        }
    }

    private static JSONObject contextBuilderWithLocalAndLibrary(@Nullable TTIdentifierFactory.AdIdInfo adIdInfo) {
        JSONObject jsonObject = contextBuilder(adIdInfo, false);
        JSON.putObject(jsonObject, "locale", SystemInfoUtil.getLocale());

        JSONObject library = JSON.build();
        JSON.putObject(library, "name", "tiktok/" + SystemInfoUtil.getLibraryName());
        JSON.putObject(library, "version", SystemInfoUtil.getSDKVersion());

        JSON.putObject(jsonObject, "library", library);
        return jsonObject;
    }

    private static JSONObject contextBuilder(@Nullable TTIdentifierFactory.AdIdInfo adIdInfo, boolean isDDL) {
        JSONObject app = TTRequest.getAppInfo();
        try {
            JSON.putObject(app, "anonymous_id", TTUserInfo.sharedInstance.anonymousId);
        } catch (Throwable ignore) {
        }

        JSONObject device = JSON.build();
        try {
            JSON.putObject(device, "platform", "Android");
            JSON.putObject(device, "os_version", SystemInfoUtil.getAndroidVersion());
            if (adIdInfo != null) {
                JSON.putObject(device, "gaid", adIdInfo.getAdId());
            }
            addDeviceInfo(device);
        } catch (Throwable ignore) {
        }

        JSONObject context = JSON.build();
        JSON.putObject(context, "app", app);
        JSON.putObject(context, "device", device);
        try {
            if (SystemInfoUtil.getInstallReferrer() != null) {
                JSONObject ad = JSON.build();
                JSON.putObject(ad, "gp_referrer", SystemInfoUtil.getInstallReferrer().getGoogleInstallReferrer());
                JSON.putObject(context, "ad", ad);
            }
        } catch (Throwable ignore) {
        }
        if (isDDL) {
            return context;
        }

        try {
            JSON.putObject(context, "ip", SystemInfoUtil.getLocalIpAddress());
            String userAgent = SystemInfoUtil.getUserAgent();
            if (userAgent != null) {
                JSON.putObject(context, "user_agent", userAgent);
            }
        } catch (Throwable ignore) {
        }

        return context;
    }

    private static JSONObject enrichDeviceBase(JSONObject d) {
        JSONObject device = d == null ? JSON.build() : d;
        try {
            JSON.putObject(device, "id", TTUtil.getOrGenAnoId(TikTokBusinessSdk.getApplicationContext(), false));
            JSON.putObject(device, "user_agent", SystemInfoUtil.getUserAgent());
            JSON.putObject(device, "ip", SystemInfoUtil.getLocalIpAddress());
            JSON.putObject(device, "network", SystemInfoUtil.getNetworkClass(TikTokBusinessSdk.getApplicationContext()));
            JSON.putObject(device, "session", TikTokBusinessSdk.getSessionID());
            JSON.putObject(device, "locale", SystemInfoUtil.getBcp47Language());
            JSON.putLong(device, "ts", System.currentTimeMillis() - SystemClock.elapsedRealtime());
            addDeviceInfo(device);
        } catch (Throwable ignore) {
        }
        return device;
    }

    private static void addDeviceInfo(JSONObject device) {
        try {
            JSON.putObject(device, "locale", SystemInfoUtil.getBcp47Language());
            JSON.putInt(device, "screen_width", SystemInfoUtil.getsScreenWidth());
            JSON.putInt(device, "screen_height", SystemInfoUtil.getsScreenHeight());
            JSON.putDouble(device, "scale", SystemInfoUtil.getsDensity());
            JSON.putObject(device, "model", Build.MODEL);
            JSON.putObject(device, "version", Build.VERSION.RELEASE);
            JSON.putObject(device, "installer_package", SystemInfoUtil.getInstallSource());
        } catch (Throwable ignore) {
        }
    }

    public static JSONObject getHealthMonitorBase() {
        if (healthBasePayloadCache != null) {
            JSON.putObject(healthBasePayloadCache, "device",
                    enrichDeviceBase(JSON.getJsonObject(healthBasePayloadCache, "device")));
            JSON.putObject(healthBasePayloadCache, "timestamp", TimeUtil.getISO8601Timestamp(new Date()));
            return healthBasePayloadCache;
        }


        JSONObject finalObj = JSON.build();

        try {
            JSONObject api = JSON.build(getImmutableContextForApi(null).toString());

            JSONObject app = JSON.getJsonObject(api, "app");
            if (app == null) {
                app = JSON.build();
            }
            JSON.putObject(app, "app_namespace", SystemInfoUtil.getPackageName());

            JSON.putObject(finalObj, "app", app);
            JSON.putObject(finalObj, "library", JSON.getJsonObject(api, "library"));
            JSON.putObject(finalObj, "device", enrichDeviceBase(JSON.getJsonObject(api, "device")));
            JSON.putObject(finalObj, "log_extra", null);

            healthBasePayloadCache = finalObj;
            JSON.putObject(healthBasePayloadCache, "timestamp", TimeUtil.getISO8601Timestamp(new Date()));
        } catch (Throwable ignore) {
        }

        return healthBasePayloadCache;
    }
}