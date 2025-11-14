package com.tiktok.iap.billing.client;

import com.tiktok.iap.billing.GPBillVersions;

class EmptyBillingProxy implements IBillingProxy {
    @Override
    public GPBillVersions.GPBillingVer getVersion() {
        return GPBillVersions.GPBillingVer.NONE;
    }

    @Override
    public void init() {

    }

    @Override
    public void queryPurchaseHistory() {

    }
}
