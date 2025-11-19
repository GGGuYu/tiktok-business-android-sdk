package com.tiktok.appevents.edp.proxy;

import android.view.View;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TouchProxyHelper {

    public static void proxy(WeakReference<View> v, ITouchListener clickListener) {
        try {
            if (clickListener == null) {
                return;
            }
            View view = v.get();
            if (view == null) {
                return;
            }
            Method method = View.class.getDeclaredMethod("getListenerInfo");
            method.setAccessible(true);
            Object mListenerInfo = method.invoke(view);
            Class<?> clz = Class.forName("android.view.View$ListenerInfo");
            Field field = clz.getDeclaredField("mOnTouchListener");
            field.setAccessible(true);
            View.OnTouchListener onTouchListenerInstance = (View.OnTouchListener) field.get(mListenerInfo);
            if (onTouchListenerInstance instanceof ProxyOnTouchListener) {
                return;
            }
            field.set(mListenerInfo, new ProxyOnTouchListener(clickListener, onTouchListenerInstance));
        } catch (Throwable ignore) {
        }
    }
}
