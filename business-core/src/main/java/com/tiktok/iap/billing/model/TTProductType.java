package com.tiktok.iap.billing.model;

public enum TTProductType {
    IN_APP("inapp"),
    SUBS("subs");


    private String type;

    TTProductType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.valueOf(this.type);
    }
}
