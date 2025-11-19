package com.tiktok.util;

import com.tiktok.TikTokBusinessSdk;

public class UrlConst {
    public static final String HTTPS = "https://";

    public static final String PATH_CONFIG = "/api/v1/app_sdk/config";
    public static final String PATH_CONFIG2 = "/api/v1/app_sdk/cache/config";
    public static final String PATH_DDL = "/api/v1/app_sdk/ddl";
    public static final String PATH_MONITOR = "/api/v1/app_sdk/monitor";
    public static final String PATH_BATCH = "/api/v1/app_sdk/batch";

    public static String getConfigUrl() {
        return HTTPS + "analytics.us.tiktok.com" + PATH_CONFIG2;
    }

    public static String getDebugModeUrl() {
        return HTTPS + "analytics.us.tiktok.com" + PATH_CONFIG;
    }

    public static String getDDLUrl() {
        return HTTPS + TikTokBusinessSdk.getApiTrackDomain() + PATH_DDL;
    }

    public static String getMonitorUrl() {
        return HTTPS + TikTokBusinessSdk.getApiTrackDomain() + PATH_MONITOR;
    }

    public static String getBatchUrl() {
        return HTTPS + TikTokBusinessSdk.getApiTrackDomain() + PATH_BATCH;
    }
}
