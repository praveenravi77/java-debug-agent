package com.praveen.javaagent;

import net.bytebuddy.implementation.bind.annotation.Origin;

import java.lang.reflect.Method;

public class ToStringInterceptor {
    public static String intercept(@Origin Method m) {
        return "Hello World from " + m.getName() + "!";
    }
}
