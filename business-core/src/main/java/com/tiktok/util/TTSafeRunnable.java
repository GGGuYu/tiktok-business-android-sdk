package com.tiktok.util;

public abstract class TTSafeRunnable implements Runnable {
    @Override
    public void run() {
        try {
            doSafeRun();
        } catch (Throwable ignore) {
        }
    }

    public abstract void doSafeRun();
}
