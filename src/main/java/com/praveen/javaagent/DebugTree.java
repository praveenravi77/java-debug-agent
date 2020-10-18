package com.praveen.javaagent;

import java.util.ArrayList;

public class DebugTree {

    public static ThreadLocal<ArrayList<String>> state = new ThreadLocal<>();

    public static void addMethodCall(String methodName) {
        if (state.get() == null) { state.set(new ArrayList<>()); }
        state.get().add(methodName);
    }

    public static String getTree() {
        return state.get().toString();
    }
}
