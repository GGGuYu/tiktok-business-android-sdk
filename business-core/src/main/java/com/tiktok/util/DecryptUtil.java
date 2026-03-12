package com.tiktok.util;

import static com.tiktok.util.TTConst.TTSDK_EXCEPTION_SDK_CATCH;

import android.text.TextUtils;
import android.util.Base64;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.appevents.TTCrashHandler;

import java.security.MessageDigest;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class DecryptUtil {
    private static final String TAG = "DecryptUtil";

    public static String encryptWithHmac(String message) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(TikTokBusinessSdk.getAccessToken().getBytes(), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);
            String token = Base64.encodeToString(mac.doFinal(message.getBytes()), Base64.NO_WRAP);
            return token;
        } catch (Throwable e) {
            return "";
        }
    }

    public static String toSha256(String str) {
        if (str == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(str.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : md.digest()) {
                result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return result.toString();
        } catch (Throwable e) {
            TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_SDK_CATCH);
        }
        return null;
    }

    public static boolean isSHA256(String data) {
        if (TextUtils.isEmpty(data)) {
            return false;
        }
        try {
            final Pattern pattern = Pattern.compile("[A-Fa-f0-9]{64}");
            return pattern.matcher(data).matches();
        } catch (Throwable ignore) {
        }
        return false;
    }
}
