package com.tiktok.util;

import android.os.Parcel;

import java.io.Closeable;

public final class IOUtils {

    public static void close(Closeable... closeables) {
        try {
            if (closeables != null) {
                for (Closeable closeable : closeables) {
                    try {
                        closeable.close();
                    } catch (Throwable ignore) {
                    }
                }
            }
        } catch (Throwable ignore) {
        }
    }

    public static void close(Parcel... parcels) {
        try {
            if (parcels != null) {
                for (Parcel parcel : parcels) {
                    try {
                        parcel.recycle();
                    } catch (Throwable ignore) {
                    }
                }
            }
        } catch (Throwable ignore) {
        }
    }
}
