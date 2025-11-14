package com.tiktok.iap.billing.client;

import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BillUtils {
    private static final Pattern P_JSON = Pattern.compile("jsonString='(.*?)'");


    public static String parserJsonFromProductDetail(String data) {
        if (TextUtils.isEmpty(data)) {
            return null;
        }

        if (!data.contains("jsonString=")) {
            return null;
        }

        try {
            Matcher matcher = P_JSON.matcher(data);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Throwable ignore) {
        }

        return null;
    }
}
