package com.tiktok.iap.billing.client;

import com.tiktok.iap.billing.GPBillVersions;

public class TTBillingFactory {

    public static IBillingProxy createBillingProxy() {
        GPBillVersions.GPBillingVer ver = GPBillVersions.getMajorVersion();
        if (ver == GPBillVersions.GPBillingVer.V5_V8) {
            return new V5_V8BillingProxy();
        }

        return new EmptyBillingProxy();
    }

}

