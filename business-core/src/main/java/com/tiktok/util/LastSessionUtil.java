package com.tiktok.util;

import android.text.TextUtils;

import com.tiktok.TikTokBusinessSdk;

import org.json.JSONObject;

public class LastSessionUtil {
    private static final String KEY_LAST_SESSION_ID = "last_session_id";
    private static final String KEY_LAST_SESSION_TIME = "last_session_time";

    private static final TTLogger sLogger = new TTLogger("LastSessionUtil", TikTokBusinessSdk.getLogLevel());


    private static volatile TTKeyValueStore sStore;
    private static volatile String sLastSessionId;
    private static volatile String sLastSessionTime;

    static {
        initSPWhenNull();
    }

    private static void initSPWhenNull() {
        try {
            if (sStore == null) {
                synchronized (LastSessionUtil.class) {
                    if (sStore == null) {
                        sStore = new TTKeyValueStore(TikTokBusinessSdk.getApplicationContext());
                        if (sStore != null) {
                            final String id = sStore.get(KEY_LAST_SESSION_ID);
                            final String time = sStore.get(KEY_LAST_SESSION_TIME);
                            if (!TextUtils.isEmpty(id) && !TextUtils.isEmpty(time)) {
                                sLastSessionId = id;
                                sLastSessionTime = time;
                            }

                            sLogger.info("Last session id: %s, time: %s", sLastSessionId, sLastSessionTime);
                        }
                    }
                }
            }
        } catch (Throwable ignore) {
        }
    }

    public static String getLastSessionID() {
        initSPWhenNull();

        return sLastSessionId;
    }

    public static String getLastSessionTime() {
        initSPWhenNull();

        return sLastSessionTime;
    }

    public static void setLast(String uuid) {
        initSPWhenNull();

        try {
            if (!TextUtils.isEmpty(uuid)) {
                final TTKeyValueStore store = sStore;
                if (store != null) {
                    store.set(KEY_LAST_SESSION_ID, uuid);
                    store.set(KEY_LAST_SESSION_TIME, TimeUtil.getISO8601Timestamp());
                }

                sLogger.info("set last session id: %s", uuid);
            }
        } catch (Throwable ignore) {
        }
    }

    public static void inject2RequestParam(JSONObject json) {
        initSPWhenNull();

        try {
            if (json != null) {
                final String id = getLastSessionID();
                final String time = getLastSessionTime();
                if (!TextUtils.isEmpty(id) && !TextUtils.isEmpty(time)) {
                    JSON.putObject(json, "last_app_session_id", id);
                    JSON.putObject(json, "last_app_session_start_time", time);
                }
            }
        } catch (Throwable ignore) {
        }
    }

}
