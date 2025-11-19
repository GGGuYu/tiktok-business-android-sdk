package com.tiktok.iap.billing.client;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryRecord;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchaseHistoryParams;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.tiktok.TikTokBusinessSdk;
import com.tiktok.appevents.TTPurchaseInfo;
import com.tiktok.appevents.edp.EDPConfig;
import com.tiktok.appevents.edp.TTEDPEventTrack;
import com.tiktok.iap.TTInAppPurchaseWrapper;
import com.tiktok.iap.billing.GPBillVersions;
import com.tiktok.iap.billing.model.TTPayData;
import com.tiktok.util.JSON;
import com.tiktok.util.TTLogger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

class V5_V8BillingProxy implements IBillingProxy {
    private static final TTLogger ttLogger = new TTLogger("BillingProxyV5", TikTokBusinessSdk.getLogLevel());

    private final AtomicBoolean mIsInitLoading = new AtomicBoolean(false);
    private final AtomicBoolean mInitSuccess = new AtomicBoolean(false);
    private volatile BillingClient mBillingClient;

    private final Map<String, TTPayData> mHistorySubs = new ConcurrentHashMap<>();
    private final Map<String, TTPayData> mHistoryInApp = new ConcurrentHashMap<>();
    private final Map<String, JSONObject> mProductDetails = new ConcurrentHashMap<>();


    private final PurchasesUpdatedListener mUpdateListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
            sendPageShow(billingResult, list);
            sendPurchase(billingResult, list);

            ttLogger.info("on billing result: " + String.valueOf(billingResult));
        }
    };

    @Override
    public GPBillVersions.GPBillingVer getVersion() {
        return GPBillVersions.GPBillingVer.V5_V8;
    }

    @Override
    public void init() {
        tryCreateAndStartBillingClient();
    }

    private boolean isAutoIAPTrackEnable() {
        return TTInAppPurchaseWrapper.autoTrackPaymentEnable;
    }

    private void sendPurchase(BillingResult billingResult, List<Purchase> list) {
        if (billingResult == null || list == null
                || billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            return;
        }

        if (isAutoIAPTrackEnable()) {
            for (Purchase purchase : list) {
                if (purchase == null) {
                    continue;
                }
                List<String> skus = purchase.getSkus();
                if (skus == null || skus.isEmpty()) {
                    continue;
                }
                querySkuAndTrack(skus, purchase, true);
            }
        }
    }

    private void querySkuAndTrack(List<String> skus, Purchase purchase, boolean isInAppPurchase) {
        try {
            List<String> skuList = new ArrayList<>();
            for (String sku : skus) {
                if (sku == null || sku.isEmpty()) {
                    continue;
                }
                skuList.add(sku);
            }
            SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
            if (isInAppPurchase) {
                params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
            } else {
                params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS);
            }
            mBillingClient.querySkuDetailsAsync(params.build(), (billingResult, skuDetailsList) -> {
                try {
                    if (billingResult != null && billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                            && skuDetailsList != null) {
                        if (skuDetailsList.size() > 0) {
                            List<TTPurchaseInfo> purchaseInfos = new ArrayList<>();
                            try {
                                for (SkuDetails skuDetails : skuDetailsList) {
                                    try {
                                        TTPurchaseInfo purchaseInfo = new TTPurchaseInfo(JSON.build(purchase.getOriginalJson()),
                                                JSON.build(skuDetails.getOriginalJson()));
                                        purchaseInfo.setAutoTrack(true);
                                        purchaseInfos.add(purchaseInfo);
                                    } catch (Throwable ignore) {
                                    }
                                }
                                TikTokBusinessSdk.trackGooglePlayPurchase(purchaseInfos);
                            } catch (Throwable e) {
                                ttLogger.error(e, "query Sku And Track google play purchase error");
                            }
                        } else {
                            if (isInAppPurchase) {
                                querySkuAndTrack(skus, purchase, false);
                            } else {
                                sendNoSkuIapTrack(skus, purchase);
                            }
                        }
                    } else {
                        sendNoSkuIapTrack(skus, purchase);
                    }
                } catch (Throwable e) {
                    ttLogger.error(e, "query Sku And Track error");
                }
            });
        } catch (Throwable e) {
            ttLogger.error(e, "query Sku And Track error2");
        }
    }

    private static void sendNoSkuIapTrack(List<String> skus, Purchase purchase) {
        try {
            JSONArray contents = JSON.buildArr();
            for (String sku : skus) {
                if (sku == null || sku.isEmpty()) {
                    continue;
                }
                JSONObject item = JSON.build();
                JSON.putInt(item, "quantity", purchase.getQuantity());
                JSON.putObject(item, "content_id", sku);

                JSON.putArr(contents, item);
            }
            JSONObject content = JSON.build();
            JSON.putObject(content, "contents", contents);
            TikTokBusinessSdk.trackEvent("Purchase", content);
        } catch (Throwable e) {
            ttLogger.error(e, "Track Purchase error");
        }
    }

    private void sendPageShow(BillingResult billingResult, List<Purchase> list) {
        if (billingResult == null || list == null) {
            return;
        }
        try {
            if (EDPConfig.enable_pay_show_track) {
                JSONArray arrPurchase = JSON.buildArr();
                for (Purchase purchase : list) {
                    JSONObject json = JSON.build(purchase.getOriginalJson());
                    JSON.putArr(arrPurchase, json);
                }
                TTEDPEventTrack.trackPayShow(billingResult.getResponseCode(), arrPurchase);
            }
        } catch (Throwable ignore) {
        }
    }

    @Override
    public void queryPurchaseHistory() {
        if (!TTInAppPurchaseWrapper.autoTrackPaymentHistory) {
            return;
        }
        if (!isStartSuccess()) {
            tryCreateAndStartBillingClient();
            return;
        }

        try {
            doQueryPurchaseHistory();
        } catch (Throwable ignore) {
        }
    }

    private void doQueryPurchaseHistory() {
        try {
            if (TTInAppPurchaseWrapper.autoTrackPaymentHistory && TTInAppPurchaseWrapper.canTrackINAPP()) {
                QueryPurchaseHistoryParams paramsINAPP = QueryPurchaseHistoryParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build();
                mBillingClient.queryPurchaseHistoryAsync(paramsINAPP, new PurchaseHistoryResponseListener() {
                    @Override
                    public void onPurchaseHistoryResponse(@NonNull BillingResult billingResult, @Nullable List<PurchaseHistoryRecord> list) {
                        if (TTInAppPurchaseWrapper.autoTrackPaymentHistory && TTInAppPurchaseWrapper.canTrackINAPP()) {
                            queryProductDetailHistory(false, list);
                        }
                    }
                });
            }
        } catch (Throwable ignore) {
        }


        try {
            if (TTInAppPurchaseWrapper.autoTrackPaymentHistory && TTInAppPurchaseWrapper.canTrackSUBS()) {
                QueryPurchaseHistoryParams paramsSUBS = QueryPurchaseHistoryParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build();
                mBillingClient.queryPurchaseHistoryAsync(paramsSUBS, new PurchaseHistoryResponseListener() {
                    @Override
                    public void onPurchaseHistoryResponse(@NonNull BillingResult billingResult, @Nullable List<PurchaseHistoryRecord> list) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            if (TTInAppPurchaseWrapper.autoTrackPaymentHistory && TTInAppPurchaseWrapper.canTrackSUBS()) {
                                queryProductDetailHistory(true, list);
                            }
                        }
                    }
                });
            }
        } catch (Throwable ignore) {
        }
    }

    private void queryProductDetailHistory(boolean isSubs, List<PurchaseHistoryRecord> list) {
        try {
            if (list == null || list.isEmpty()) {
                return;
            }

            final List<String> idList = new ArrayList<>();
            for (PurchaseHistoryRecord history : list) {
                try {
                    String data = history.getOriginalJson();
                    JSONObject json = JSON.build(data);
                    String pid = JSON.getString(json, "productId");
                    if (!TextUtils.isEmpty(pid)) {
                        TTPayData pay = new TTPayData();
                        pay.productId = pid;
                        pay.data = json;
                        pay.purchaseTime = history.getPurchaseTime();
                        if (isSubs) {
                            mHistorySubs.put(pid, pay);
                        } else {
                            mHistoryInApp.put(pid, pay);
                        }

                        if (!mProductDetails.containsKey(pid)) {
                            idList.add(pid);
                        }
                    }
                } catch (Throwable ignore) {
                }
            }

            if (idList.isEmpty()) {
                //直接上报
                tryUploadHistoryLog();
            } else {
                doQueryProductDetails(isSubs, idList);
            }
        } catch (Throwable ignore) {
        }
    }

    private void tryUploadHistoryLog() {
        if (!TTInAppPurchaseWrapper.autoTrackPaymentHistory) {
            return;
        }
        if (mHistorySubs.isEmpty() && mHistoryInApp.isEmpty() && mProductDetails.isEmpty()) {
            return;
        }

        try {
            if (TTInAppPurchaseWrapper.canTrackSUBS()) {
                Map<String, TTPayData> map = new HashMap<>(mHistorySubs);
                map = filterPurchase(true, map);
                sendHistoryLog(true, map);
            }
        } catch (Throwable ignore) {
        }


        if (TTInAppPurchaseWrapper.canTrackINAPP()) {
            Map<String, TTPayData> map = new HashMap<>(mHistoryInApp);
            map = filterPurchase(false, map);
            sendHistoryLog(false, map);
        }

        mHistorySubs.clear();
        mHistoryInApp.clear();
    }

    private void sendHistoryLog(boolean isSubs, Map<String, TTPayData> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        try {
            List<TTPurchaseInfo> list = new ArrayList<>();
            for (Map.Entry<String, TTPayData> entry : map.entrySet()) {
                try {
                    String pid = entry.getKey();
                    TTPayData payData = entry.getValue();
                    JSONObject sku = mProductDetails.get(pid);
                    if (sku != null && sku.length() > 0) {
                        TTPurchaseInfo info = new TTPurchaseInfo(payData.data, sku);
                        info.setAutoTrack(true);
                        list.add(info);
                    }
                } catch (Throwable ignore) {
                }
            }
            if (!list.isEmpty()) {
                TikTokBusinessSdk.trackGooglePlayPurchase(list);
            }
        } catch (Throwable ignore) {
        }
    }

    private Map<String, TTPayData> filterPurchase(boolean isSubs, Map<String, TTPayData> map) {
        Map<String, TTPayData> filterMap = new HashMap<>();

        try {
            // filter purchase time
            if (map != null && !map.isEmpty()) {
                long last = isSubs ? BillCache.getInstance().getSUBSLast() : BillCache.getInstance().getINAPPLast();
                for (Map.Entry<String, TTPayData> entry : map.entrySet()) {
                    try {
                        TTPayData pay = entry.getValue();
                        if (pay != null) {
                            if (pay.purchaseTime > last) {
                                filterMap.put(entry.getKey(), entry.getValue());
                            }
                        }
                    } catch (Throwable ignore) {
                    }
                }
            }
        } catch (Throwable ignore) {
        }

        try {
            //filter total number
            List<TTPayData> allPays = new ArrayList<>();
            for (Map.Entry<String, TTPayData> entry : filterMap.entrySet()) {
                if (entry != null && entry.getValue() != null) {
                    allPays.add(entry.getValue());
                }
            }

            //按照时间倒序
            Collections.sort(allPays, new Comparator<TTPayData>() {
                @Override
                public int compare(TTPayData o1, TTPayData o2) {
                    return o1 == null || o2 == null ? 0 : Long.valueOf(o2.purchaseTime - o1.purchaseTime).intValue();
                }
            });

            filterMap = new HashMap<>();
            int total = isSubs ? TTInAppPurchaseWrapper.autoTrackPaymentHistorySUBS : TTInAppPurchaseWrapper.autoTrackPaymentHistoryINAPP;
            total = Math.min(allPays.size(), total);
            for (int i = 0; i < total; i++) {
                try {
                    TTPayData pay = allPays.get(i);
                    if (pay != null && !TextUtils.isEmpty(pay.productId)) {
                        filterMap.put(pay.productId, pay);
                    }
                } catch (Throwable ignore) {
                }
            }
        } catch (Throwable ignore) {
        }

        return filterMap;
    }

    private void doQueryProductDetails(boolean isSubs, List<String> idList) {
        if (idList == null || idList.isEmpty()) {
            return;
        }

        List<QueryProductDetailsParams.Product> products = new ArrayList<>();
        for (String pid : idList) {
            QueryProductDetailsParams.Product product = QueryProductDetailsParams.Product.newBuilder()
                    .setProductType(isSubs ? BillingClient.ProductType.SUBS : BillingClient.ProductType.INAPP)
                    .setProductId(pid)
                    .build();
            products.add(product);
        }

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(products)
                .build();
        mBillingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull List<ProductDetails> list) {
                if (billingResult != null && billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    if (list != null && !list.isEmpty()) {
                        for (ProductDetails detail : list) {
                            try {
                                if (detail != null) {
                                    String jsonStr = BillUtils.parserJsonFromProductDetail(detail.toString());
                                    JSONObject json = JSON.build(jsonStr);
                                    if (json != null && json.length() > 0) {
                                        mProductDetails.put(detail.getProductId(), json);
                                    }
                                }
                            } catch (Throwable ignore) {
                            }
                        }

                        tryUploadHistoryLog();
                    }
                }
            }
        });
    }

    private boolean isStartSuccess() {
        return !mIsInitLoading.get() && mInitSuccess.get() && mBillingClient != null && mBillingClient.isReady();
    }

    private void tryCreateAndStartBillingClient() {
        if (isStartSuccess()) {
            return;
        }

        mIsInitLoading.set(true);

        try {
            mBillingClient = BillingClient.newBuilder(TikTokBusinessSdk.getApplicationContext())
                    .setListener(mUpdateListener)
                    .enablePendingPurchases()
                    .build();
            mBillingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingServiceDisconnected() {
                    mIsInitLoading.set(false);
                    mInitSuccess.set(false);
                }

                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    mIsInitLoading.set(false);
                    mInitSuccess.set(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK);
                }
            });
        } catch (Throwable ignore) {
            mIsInitLoading.set(false);
            mInitSuccess.set(false);
        }
    }

}
