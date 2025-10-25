package com.tiktok.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class TTReflect {

    private Class<?> mClass;
    private Method mMethod;
    private Field mField;

    public static TTReflect on(String className) {
        TTReflect reflect = new TTReflect();
        try {
            reflect.mClass = Class.forName(className);
        } catch (Throwable ignore) {
        }
        return reflect;
    }

    public static TTReflect on(Class<?> clazz) {
        TTReflect reflect = new TTReflect();
        reflect.mClass = clazz;
        return reflect;
    }

    private TTReflect() {
    }

    public TTReflect findField(String fieldName) {
        try {
            mField = mClass.getDeclaredField(fieldName);
        } catch (Throwable ignore) {
        }
        return this;
    }

    public Object getValue(Object receiver) {
        try {
            mField.setAccessible(true);
            return mField.get(receiver);
        } catch (Throwable ignore) {
        }
        return null;
    }

    public TTReflect findMethod(String methodName, Class<?>... parameterTypes) {
        try {
            mMethod = mClass.getDeclaredMethod(methodName, parameterTypes);
        } catch (Throwable ignore) {
        }
        return this;
    }

    public Object call(Object receiver, Object... args) {
        try {
            mMethod.setAccessible(true);
            return mMethod.invoke(receiver, args);
        } catch (Throwable ignore) {
        }
        return null;
    }


    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (Throwable ignore) {
        }
        return null;
    }

    public static Method getMethod(String className, String methodName, Class<?>... parameterTypes) {
        try {
            Class<?> clazz = Class.forName(className);
            return getMethod(clazz, methodName, parameterTypes);
        } catch (Throwable ignore) {
        }
        return null;
    }

    public static Object callMethod(Method method, Object receiver, Object... args) {
        try {
            return method.invoke(receiver, args);
        } catch (Throwable ignore) {
        }
        return null;
    }

}
