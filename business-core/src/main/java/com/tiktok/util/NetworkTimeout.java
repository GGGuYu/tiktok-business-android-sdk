package com.tiktok.util;

import org.json.JSONObject;

public class NetworkTimeout {
    private static final int DEF_CONFIG_TIME = 2000;
    private static final int DEF_EVENT_TIME = 10000;

    /**
     * Config & ddl & debug_mode
     */
    public static volatile int sConfigTime = DEF_CONFIG_TIME;

    /**
     * others: Batch & monitor. etc
     */
    public static volatile int sEventTime = DEF_EVENT_TIME;

    public static void updateConfig(JSONObject json) {
        if (json == null) {
            return;
        }

        sConfigTime = JSON.getInt(json, "network_timeout_config_interval", DEF_CONFIG_TIME);
        sEventTime = JSON.getInt(json, "network_timeout_event_interval", DEF_EVENT_TIME);
    }
    
}
