package com.tiktok.appevents;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.JSON;

import org.json.JSONObject;

class DebugModeHelper {
    private static volatile boolean sIsSuccess = false;

    public static boolean isSuccess() {
        return sIsSuccess;
    }

    public static void tryRequestConfig() {
        if (isSuccess()) {
            return;
        }

        try {
            JSONObject result = TTRequest.getDebugModeConfig();

            sIsSuccess = result != null && result.has("enable_debug_mode");

            boolean enableDebugMode = JSON.getBoolean(result, "enable_debug_mode", false);

            if (enableDebugMode) {
                TikTokBusinessSdk.enableDebugMode();
            } else {
                TikTokBusinessSdk.disableDebugMode();
            }
        } catch (Throwable ignore) {
        }
    }

}
