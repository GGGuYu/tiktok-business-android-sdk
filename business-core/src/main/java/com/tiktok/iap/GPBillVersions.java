package com.tiktok.iap;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.TTReflect;

public class GPBillVersions {

    private static volatile String sVersion;

    public static GPBillingVer getMajorVersion() {
        try {
            String version = getVersion();
            if (version != null) {
                String[] vers = version.split("\\.");
                int major = Integer.parseInt(vers[0]);
                if (major == 1) {
                    return GPBillingVer.V1;
                } else if (major > 1 && major < 5) {
                    return GPBillingVer.V2_V4;
                } else {
                    return GPBillingVer.V5_V8;
                }
            }
        } catch (Throwable ignore) {
        }
        return GPBillingVer.NONE;
    }

    public static String getVersion() {
        if (!TextUtils.isEmpty(sVersion)) {
            return sVersion;
        }

        sVersion = readFromMeta();
        if (!TextUtils.isEmpty(sVersion)) {
            return sVersion;
        }

        sVersion = readFromBuildConfig();
        if (!TextUtils.isEmpty(sVersion)) {
            return sVersion;
        }

        return "";
    }

    private static String readFromMeta() {
        try {
            Context context = TikTokBusinessSdk.getApplicationContext();
            if (context != null) {
                context.getPackageManager().getInstallerPackageName("");
                ApplicationInfo info = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                String ver = info.metaData.getString("com.google.android.play.billingclient.version", null);
                if (ver != null && ver.length() > 2) {
                    return ver;
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static String readFromBuildConfig() {
        try {
            String ver = (String) TTReflect.on("com.android.billingclient.BuildConfig")
                    .findField("VERSION_NAME")
                    .getValue(null);
            if (ver != null && ver.length() > 2) {
                return ver;
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    public enum GPBillingVer {
        NONE, V1, V2_V4, V5_V8
    }

}
