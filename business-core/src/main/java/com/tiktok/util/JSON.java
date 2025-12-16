package com.tiktok.util;

import android.text.TextUtils;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

@Keep
public final class JSON {
    private static final String TAG = "JSON";

    @NonNull
    public static JSONObject build() {
        return new JSONObject();
    }

    @Nullable
    public static JSONObject build(String json) {
        try {
            return new JSONObject(json);
        } catch (Throwable ignore) {
        }
        return null;
    }

    @Nullable
    public static JSONObject build(Map<String, String> map) {
        try {
            return new JSONObject(map);
        } catch (Throwable ignore) {
        }
        return null;
    }

    @NonNull
    public static JSONArray buildArr() {
        return new JSONArray();
    }

    @Nullable
    public static JSONArray buildArr(String json) {
        try {
            return new JSONArray(json);
        } catch (Throwable ignore) {
        }
        return null;
    }

    public static void putInt(JSONObject json, String key, int data) {
        try {
            if (json != null && !TextUtils.isEmpty(key)) {
                json.put(key, data);
            }
        } catch (Throwable ignore) {
        }
    }

    public static void putLong(JSONObject json, String key, long data) {
        try {
            if (json != null && !TextUtils.isEmpty(key)) {
                json.put(key, data);
            }
        } catch (Throwable ignore) {
        }
    }

    public static void putDouble(JSONObject json, String key, double data) {
        try {
            if (json != null && !TextUtils.isEmpty(key)) {
                json.put(key, data);
            }
        } catch (Throwable ignore) {
        }
    }

    public static void putBoolean(JSONObject json, String key, boolean data) {
        try {
            if (json != null && !TextUtils.isEmpty(key)) {
                json.put(key, data);
            }
        } catch (Throwable ignore) {
        }
    }

    public static void putObject(JSONObject json, String key, Object data) {
        try {
            if (json != null && !TextUtils.isEmpty(key)) {
                json.put(key, data);
            }
        } catch (Throwable ignore) {
        }
    }

    public static void putArr(JSONArray arr, Object object) {
        try {
            if (arr != null && object != null) {
                arr.put(object);
            }
        } catch (Throwable ignore) {
        }
    }

    public static int getInt(JSONObject json, String key) {
        return getInt(json, key, 0);
    }

    public static int getInt(JSONObject json, String key, int fallback) {
        try {
            if (json != null && !TextUtils.isEmpty(key)) {
                return json.optInt(key, fallback);
            }
        } catch (Throwable ignore) {
        }
        return fallback;
    }

    public static long getLong(JSONObject json, String key) {
        return getLong(json, key, 0L);
    }

    public static long getLong(JSONObject json, String key, long fallback) {
        try {
            if (json != null && !TextUtils.isEmpty(key)) {
                return json.optLong(key, fallback);
            }
        } catch (Throwable ignore) {
        }
        return fallback;
    }

    public static double getDouble(JSONObject json, String key) {
        return getDouble(json, key, 0.F);
    }

    public static double getDouble(JSONObject json, String key, double fallback) {
        try {
            if (json != null && !TextUtils.isEmpty(key)) {
                return json.optDouble(key, fallback);
            }
        } catch (Throwable ignore) {
        }
        return fallback;
    }

    public static String getString(JSONObject json, String key) {
        return getString(json, key, "");
    }

    public static String getString(JSONObject json, String key, String fallback) {
        try {
            if (json != null && !TextUtils.isEmpty(key)) {
                return json.optString(key, fallback);
            }
        } catch (Throwable ignore) {
        }
        return fallback;
    }

    public static boolean getBoolean(JSONObject json, String key) {
        return getBoolean(json, key, false);
    }

    public static boolean getBoolean(JSONObject json, String key, boolean fallback) {
        try {
            if (json != null && !TextUtils.isEmpty(key)) {
                return json.optBoolean(key, fallback);
            }
        } catch (Throwable ignore) {
        }
        return fallback;
    }

    public static Object getObject(JSONObject json, String key) {
        return getObject(json, key, null);
    }

    public static Object getObject(JSONObject json, String key, Object fallback) {
        try {
            if (json != null && !TextUtils.isEmpty(key)) {
                return json.opt(key);
            }
        } catch (Throwable ignore) {
        }
        return fallback;
    }

    public static JSONObject getJsonObject(JSONObject json, String key) {
        try {
            if (json != null && !TextUtils.isEmpty(key)) {
                return json.optJSONObject(key);
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    public static JSONArray getJsonArray(JSONObject json, String key) {
        try {
            if (json != null && !TextUtils.isEmpty(key)) {
                return json.optJSONArray(key);
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    public static Iterator<String> getKeys(JSONObject json) {
        try {
            if (json != null) {
                return json.keys();
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    public static JSONArray remove(JSONArray jsonArray, int index) {
        JSONArray result = new JSONArray();
        if (index < 0 || index >= jsonArray.length()) {
            return jsonArray;
        }
        for (int i = 0; i < index; i++) {
            try {
                result.put(jsonArray.getJSONObject(i));
            } catch (JSONException ignored) {
            }
        }

        for (int i = index + 1; i < jsonArray.length(); i++) {
            try {
                result.put(jsonArray.getJSONObject(i));
            } catch (Throwable ignored) {
            }
        }
        return result;
    }

    @Nullable
    public static JSONObject getJSONObject(JSONArray jsonArray, int index) {
        try {
            return jsonArray.getJSONObject(index);
        } catch (Throwable ignored) {
        }
        return null;
    }
}
