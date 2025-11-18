/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import static com.tiktok.appevents.TTAppEventLogger.autoTrackRetentionEnable;
import static com.tiktok.util.TTConst.AutoEvents;
import static com.tiktok.util.TTConst.TRACK_TYPE;
import static com.tiktok.util.TTConst.TRACK_TYPE_AUTO;
import static com.tiktok.util.TTConst.TTSDK_APP_2DR_TIME;
import static com.tiktok.util.TTConst.TTSDK_APP_FIRST_INSTALL;
import static com.tiktok.util.TTConst.TTSDK_APP_LAST_LAUNCH;

import android.text.TextUtils;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.JSON;
import com.tiktok.util.TTConst;
import com.tiktok.util.TTKeyValueStore;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

class TTAutoEventsManager {

    private static final String TAG = TTAutoEventsManager.class.getCanonicalName();

    private static final SimpleDateFormat dateFormat;
    private static final SimpleDateFormat timeFormat;

    static {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        timeFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
    }

    private final TTAppEventLogger appEventLogger;
    private final TTKeyValueStore store;

    public TTAutoEventsManager(TTAppEventLogger appEventLogger) {
        this.appEventLogger = appEventLogger;
        store = new TTKeyValueStore(TikTokBusinessSdk.getApplicationContext());
    }

    Boolean shouldTrackAppLifecycleEvents(TTConst.AutoEvents event) {
        return appEventLogger.lifecycleTrackEnable
                && !appEventLogger.disabledEvents.contains(event);
    }

    /**
     * the events to be tracked when the app was just activated
     * 1. InstallApp
     * 2. 2Dretention
     * 3. LaunchAPP
     */
    public void trackOnAppOpenEvents() {
        trackFirstInstallEvent();
        track2DayRetentionEvent();
        trackLaunchEvent();
    }

    private void trackFirstInstallEvent() {
        try {
            /* get install trigger time, set only on InstallApp trigger */
            String installTime = store.get(TTSDK_APP_FIRST_INSTALL);
            if (installTime != null) return;

            Date now = new Date();
            HashMap<String, Object> hm = new HashMap<>();
            hm.put(TTSDK_APP_FIRST_INSTALL, timeFormat.format(now));

            /* check and track InstallApp. */
            if (shouldTrackAppLifecycleEvents(AutoEvents.InstallApp)) {
                try {
                    JSONObject props = JSON.build();
                    JSON.putObject(props, TRACK_TYPE, TRACK_TYPE_AUTO);
                    appEventLogger.track(AutoEvents.InstallApp.name, props);
                } catch (Throwable ignore) {
                }
            }

            store.set(hm);
        } catch (Throwable ignore) {
        }
    }

    /**
     * 2Dretention should be called at 2 places
     * 1. when the app is opened
     * 2. when the user switches to the background, and then switch back after some while,
     * since most users click "home" button rather than kill the process most of the time.
     */
    void track2DayRetentionEvent() {
        String is2DayLogged = store.get(TTSDK_APP_2DR_TIME);
        if (is2DayLogged != null) return;

        String firstInstall = store.get(TTSDK_APP_FIRST_INSTALL);
        if (TextUtils.isEmpty(firstInstall)) {
            return;// should not happen
        }

        try {
            Date firstLaunchTime = timeFormat.parse(firstInstall);
            Date now = new Date();
            if (shouldTrackAppLifecycleEvents(AutoEvents.SecondDayRetention)
                    && isSatisfyRetention(firstLaunchTime, now)
                    && autoTrackRetentionEnable) {
                try {
                    JSONObject props = JSON.build();
                    JSON.putObject(props, TRACK_TYPE, TRACK_TYPE_AUTO);
                    appEventLogger.track(AutoEvents.SecondDayRetention.name, props);
                } catch (Throwable ignore) {
                }

                try {
                    store.set(TTSDK_APP_2DR_TIME, timeFormat.format(now));
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void trackLaunchEvent() {
        if (shouldTrackAppLifecycleEvents(AutoEvents.LaunchAPP)) {
            try {
                JSONObject props = JSON.build();
                JSON.putObject(props, TRACK_TYPE, TRACK_TYPE_AUTO);
                appEventLogger.track(AutoEvents.LaunchAPP.name, props);
            } catch (Throwable ignore) {
            }

            try {
                store.set(TTSDK_APP_LAST_LAUNCH, timeFormat.format(new Date()));
            } catch (Throwable ignored) {
            }
        }
    }

    // extract into a single method to simplify writing unit test
    private boolean isSatisfyRetention(Date firstLaunch, Date now) {
        Calendar c = Calendar.getInstance();
        c.setTime(firstLaunch);
        c.add(Calendar.DATE, 1);
        String nextDayFromFirst = dateFormat.format(c.getTime());
        String todayDate = dateFormat.format(now);
        return nextDayFromFirst.equals(todayDate);
    }

}
