/*******************************************************************************
 * Copyright (c) 2025. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents.base;


import static com.tiktok.appevents.base.EventName.IMPRESSION_LEVEL_AD_REVENUE;
import org.json.JSONObject;

public class TTAdRevenueEvent extends TTBaseEvent {
    public TTAdRevenueEvent(String eventName, JSONObject properties, String eventId) {
        super(eventName, properties, eventId);
    }

    public static Builder newBuilder(JSONObject adRevenueJson) {
        Builder builder = new Builder(IMPRESSION_LEVEL_AD_REVENUE.toString());
        builder.addProperty("ad_revenue", adRevenueJson);
        return builder;
    }

    public static Builder newBuilder(JSONObject adRevenueJson, String eventId) {
        Builder builder = new Builder(IMPRESSION_LEVEL_AD_REVENUE.toString(), eventId);
        builder.addProperty("ad_revenue", adRevenueJson);
        return builder;
    }
}
