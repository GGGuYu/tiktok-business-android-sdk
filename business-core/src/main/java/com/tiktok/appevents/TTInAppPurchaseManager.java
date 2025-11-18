/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import static com.tiktok.appevents.contents.TTContentsEventConstants.Params.EVENT_PROPERTY_ORDER_ID;
import static com.tiktok.util.TTConst.TRACK_TYPE;
import static com.tiktok.util.TTConst.TRACK_TYPE_AUTO;

import com.tiktok.util.JSON;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;

class TTInAppPurchaseManager {
    static final String TAG = TTInAppPurchaseManager.class.getName();

    /**
     * p
     */
    static JSONObject getPurchaseProps(TTPurchaseInfo purchaseInfo) {
        String productId = null;
        try {
            productId = JSON.getString(purchaseInfo.getPurchase(), "productId");
            JSONObject skuDetail = purchaseInfo.getSkuDetails();
            JSONObject purchaseProperties = getPurchaseProperties(productId, skuDetail);
            if (purchaseInfo.isAutoTrack()) {
                JSON.putObject(purchaseProperties, TRACK_TYPE, TRACK_TYPE_AUTO);
                JSON.putObject(purchaseProperties, EVENT_PROPERTY_ORDER_ID, JSON.getString(purchaseInfo.getPurchase(), "orderId"));
            }
            return purchaseProperties;
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * returns content_id -> sku always
     */
    private static JSONObject getPurchaseProperties(String sku, JSONObject skuDetails) throws JSONException {
        JSONObject props = JSON.build();

        JSONObject content = JSON.build();
        JSON.putObject(content, "content_id", sku);

        if (skuDetails != null) {
            JSON.putInt(content, "quantity", 1);
            JSON.putObject(content, "content_type", safeJsonGetString(skuDetails, "type"));

            String currencyCode = safeJsonGetString(skuDetails, "price_currency_code");
            JSON.putObject(props, "currency", currencyCode);
            double dPrice = 0;
            try {
                dPrice = BigDecimal.valueOf(JSON.getLong(skuDetails, "price_amount_micros", 0) / 1000000.0).doubleValue();
            } catch (Throwable ignored) {
            }
            JSON.putDouble(content, "price", dPrice);
            JSON.putDouble(props, "value", dPrice);
        }
        JSON.putObject(props, "contents", JSON.buildArr().put(content));
        return props;
    }

    /**
     * safe get key from jsonobject
     */
    private static String safeJsonGetString(JSONObject jsonObject, String key) {
        try {
            return jsonObject.get(key).toString();
        } catch (Throwable ignore) {
            return "";
        }
    }

}
