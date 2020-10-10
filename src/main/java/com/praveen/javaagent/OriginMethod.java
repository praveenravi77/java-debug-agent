package com.praveen.javaagent;

import net.bytebuddy.implementation.bind.annotation.Origin;

import java.lang.reflect.Method;

public class OriginMethod {

    public static Object foo(@Origin(cache = false) Method method) {
        System.out.println("gets here");
        return method;
    }
}
