/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.util;

import static com.tiktok.util.TTConst.TTSDK_EXCEPTION_NET_ERROR;

import android.os.SystemClock;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.appevents.TTCrashHandler;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

public class HttpRequestUtil {
    private static final String TAG = "HttpRequestUtil";
    private static final String MONITOR_API_TYPE = "monitor";
    private static final String API_ERR = "api_err";

    private static final TTLogger ttLogger = new TTLogger(TAG, TikTokBusinessSdk.getLogLevel());


    public static class HttpRequestOptions {
        private boolean isGzip = true;
        private static final int UNSET = -1;
        public int connectTimeout = UNSET;
        public int readTimeout = UNSET;

        public void configConnection(HttpURLConnection connection) {
            if (connectTimeout != UNSET) {
                connection.setConnectTimeout(connectTimeout);
            }
            if (readTimeout != UNSET) {
                connection.setReadTimeout(readTimeout);
            }
        }
    }

    public static HttpsURLConnection connect(String url, Map<String, String> headerParamMap, HttpRequestOptions options, String method, String contentLength) {
        HttpsURLConnection connection = null;

        try {
            URL httpURL = new URL(url);
            connection = (HttpsURLConnection) httpURL.openConnection();
            connection.setRequestMethod(method);
            options.configConnection(connection);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            if (method.equals("GET")) {
                connection.setDoOutput(false);
            } else if (method.equals("POST")) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Length", contentLength);
            }

            if (headerParamMap != null && !headerParamMap.isEmpty()) {
                for (Map.Entry<String, String> entry : headerParamMap.entrySet()) {
                    final String key = entry.getKey();
                    final String value = entry.getValue();
                    if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                        connection.setRequestProperty(key, value);
                    }
                }
            }

            if (options.isGzip) {
                connection.setRequestProperty("Content-Encoding", "gzip");
            }

            connection.connect();
        } catch (Throwable e) {
            TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_NET_ERROR);
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Throwable ignore) {
                }
            }
        }
        return connection;
    }

    public static boolean shouldRedirect(int status) {
        if (status != HttpURLConnection.HTTP_OK) {
            return status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER
                    || status == 307;
        }
        return false;
    }

    public static String doGet(String url, Map<String, String> headerParamMap, HttpRequestOptions options) {
        long initTimeMS = System.currentTimeMillis();
        String result = null;
        int responseCode = 0;
        String apiType = "";
        String message = "";
        try {
            URL uri = new URL(url);
            apiType = uri.getPath().split("/app_sdk/")[1];
        } catch (Throwable e) {
            message = e.getMessage();
        }
        HttpsURLConnection connection = connect(url, headerParamMap, options, "GET", null);
        if (connection == null) return null;
        try {
            boolean redirect = shouldRedirect(connection.getResponseCode());
            if (redirect) {
                String redirectUrl = connection.getHeaderField("Location");
                connection.disconnect();
                connection = connect(redirectUrl, headerParamMap, options, "GET", null);
            }

            responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                result = streamToString(connection.getInputStream());
            }
        } catch (Throwable e) {
            message = e.getMessage();
            TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_NET_ERROR);
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Throwable ignore) {
                }
            }
        }
        long endTimeMS = System.currentTimeMillis();
        try {
            int dataCodeFromApi = getCodeFromApi(result);
            if (dataCodeFromApi != 0) {
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    responseCode = dataCodeFromApi;
                    message = getMessageFromApi(result);
                }
                JSONObject meta = TTUtil.getMetaWithTS(initTimeMS);
                JSON.putLong(meta, "latency", endTimeMS - initTimeMS);
                JSON.putObject(meta, "api_type", apiType);
                JSON.putInt(meta, "status_code", responseCode);
                JSON.putObject(meta, "message", message);
                JSON.putObject(meta, "log_id", getLogIDFromApi(result));
                TikTokBusinessSdk.getAppEventLogger().monitorMetric(API_ERR, meta, null);
            }
        } catch (Throwable ignored) {
        }
        return result;
    }

    public static String doPost(String url, Map<String, String> headerParamMap, String jsonStr, boolean needSignature) {
        HttpRequestOptions options = new HttpRequestOptions();

        try {
            if (url.contains(UrlConst.PATH_CONFIG2)
                    || url.contains(UrlConst.PATH_DDL)
                    || url.contains(UrlConst.PATH_CONFIG)) {
                options.connectTimeout = NetworkTimeout.sConfigTime;
                options.readTimeout = NetworkTimeout.sConfigTime * 3;
            } else {
                options.connectTimeout = NetworkTimeout.sEventTime;
                options.readTimeout = NetworkTimeout.sEventTime * 3;
            }
        } catch (Throwable ignore) {
            options.connectTimeout = 2000;
            options.readTimeout = 5000;
        }

        return doPost(url, headerParamMap, jsonStr, options, needSignature);
    }

    public static String doPost(String url, Map<String, String> headerParamMap, String jsonStr) {
        return doPost(url, headerParamMap, jsonStr, true);
    }

    public static String doPost(String url, Map<String, String> headerParamMap, String jsonStr, HttpRequestOptions options, boolean needSignature) {
        long initTimeMS = System.currentTimeMillis();
        String result = null;
        int responseCode = 0;
        String apiType = "";
        String message = "";
        try {
            URL uri = new URL(url);
            apiType = uri.getPath().split("/app_sdk/")[1];
        } catch (Throwable e) {
            message = e.getMessage();
        }

        HttpURLConnection connection = null;
        OutputStream outputStream = null;

        try {
            if (needSignature) {
                String securityKey = DecryptUtil.encryptWithHmac(jsonStr);
                headerParamMap.put("X-TT-Signature", securityKey);
            } else {
                headerParamMap.remove("X-TT-Signature");
            }

            GzipInfo info = compress2Gzip(jsonStr);
            String contentLength = "0";
            byte[] writeBytes = info.bytes;
            if (writeBytes != null && writeBytes.length > 0) {
                contentLength = String.valueOf(writeBytes.length);
            } else {
                if (options != null) {
                    options.isGzip = false;
                }
                try {
                    writeBytes = jsonStr.getBytes("utf-8");
                    contentLength = String.valueOf(writeBytes.length);
                } catch (Throwable ignore) {
                }
            }

            if (writeBytes == null || writeBytes.length == 0) {
                monitorGzipData(info);
                writeBytes = new byte[]{};
            }

            connection = connect(url, headerParamMap, options, "POST", contentLength);
            if (connection == null) return null;
            outputStream = connection.getOutputStream();
            outputStream.write(writeBytes);
            outputStream.flush();
            boolean redirect = shouldRedirect(connection.getResponseCode());
            if (redirect) {
                String redirectUrl = connection.getHeaderField("Location");
                connection.disconnect();
                connection = connect(redirectUrl, headerParamMap, options, "POST", contentLength);
                outputStream = connection.getOutputStream();
                outputStream.write(writeBytes);
                outputStream.flush();
            }

            responseCode = connection.getResponseCode();
            // http code is different from the code returned by api
            if (responseCode == HttpURLConnection.HTTP_OK) {
                result = streamToString(connection.getInputStream());
            }
            if (TikTokBusinessSdk.isInSdkDebugMode()) {
                ttLogger.info("doPost request body: %s", jsonStr);
                ttLogger.info("doPost result: %s", result == null ? String.valueOf(responseCode) : result);
            }
        } catch (Throwable e) {
            message = e.getMessage();
            TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_NET_ERROR);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Throwable ignore) {
                }
            }
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Throwable ignore) {
                }
            }
        }
        long endTimeMS = System.currentTimeMillis();
        try {
            int dataCodeFromApi = getCodeFromApi(result);
            if (dataCodeFromApi != 0) {
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    responseCode = dataCodeFromApi;
                    message = getMessageFromApi(result);
                }
            }
            if (dataCodeFromApi != 0 && !url.contains(MONITOR_API_TYPE)) {
                JSONObject meta = TTUtil.getMetaWithTS(initTimeMS);
                JSON.putLong(meta, "latency", endTimeMS - initTimeMS);
                JSON.putObject(meta, "api_type", apiType);
                JSON.putInt(meta, "status_code", responseCode);
                JSON.putObject(meta, "message", message);
                JSON.putObject(meta, "log_id", getLogIDFromApi(result));
                TikTokBusinessSdk.getAppEventLogger().monitorMetric(API_ERR, meta, null);
            }
        } catch (Throwable ignored) {
        }
        return result;
    }

    private static void monitorGzipData(GzipInfo info) {
        if (info == null) {
            return;
        }

        try {
            JSONObject meta = TTUtil.getMetaWithTS(null);
            JSON.putInt(meta, "code", -1);
            JSON.putLong(meta, "duration", info.duration);
            String msg = "";
            if (info.throwable1 != null) {
                msg += info.throwable1.getMessage();
            }
            if (info.throwable2 != null) {
                msg += "==" + info.throwable2.getMessage();
            }
            JSON.putObject(meta, "err_msg", msg);
            TikTokBusinessSdk.getAppEventLogger().monitorMetric("gzip_err", meta, null);
        } catch (Throwable ignore) {
        }
    }

    private static class GzipInfo {
        public long duration;
        public byte[] bytes;
        public Throwable throwable1;
        public Throwable throwable2;
    }

    @NonNull
    private static GzipInfo compress2Gzip(String requestBody) {
        long start = SystemClock.elapsedRealtime();
        GzipInfo info = new GzipInfo();

        if (TextUtils.isEmpty(requestBody)) {
            info.duration = SystemClock.elapsedRealtime() - start;
            info.throwable1 = new Exception("request body is empty");
            return info;
        }
        ByteArrayOutputStream outputStream = null;
        GZIPOutputStream gzipOutputStream = null;
        byte[] bytes = new byte[]{};
        try {
            outputStream = new ByteArrayOutputStream();
            gzipOutputStream = new GZIPOutputStream(outputStream);
            gzipOutputStream.write(requestBody.getBytes("utf-8"));
        } catch (Throwable e) {
            info.throwable1 = e;
        } finally {
            IOUtils.close(gzipOutputStream);
            if (outputStream != null) {
                try {
                    bytes = outputStream.toByteArray();
                    info.bytes = bytes;
                } catch (Throwable e) {
                    info.throwable2 = e;
                }
                IOUtils.close(outputStream);
            }
        }

        info.duration = SystemClock.elapsedRealtime() - start;

        return info;
    }

    private static String streamToString(InputStream is) {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString().trim();
        } catch (Throwable e) {
            TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_NET_ERROR);
        }
        return null;
    }

    public static int getCodeFromApi(@Nullable String resp) {
        if (resp != null) {
            try {
                JSONObject respJson = JSON.build(resp);
                return JSON.getInt(respJson, "code", -1);
            } catch (Throwable ignored) {
                return -2;
            }
        }
        return -1;
    }

    public static String getMessageFromApi(@Nullable String resp) {
        if (resp != null) {
            try {
                JSONObject respJson = JSON.build(resp);
                return JSON.getString(respJson, "message");
            } catch (Throwable e) {
                return e.getMessage();
            }
        }
        return "result is empty";
    }

    public static String getLogIDFromApi(@Nullable String resp) {
        if (resp != null) {
            try {
                JSONObject respJson = JSON.build(resp);
                return JSON.getString(respJson, "request_id");
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }
}