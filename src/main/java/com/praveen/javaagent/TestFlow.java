package com.praveen.javaagent;

import java.util.ArrayList;

public class TestFlow {
    public static void main(String[] args) {
        TestController testController = new TestController();
        new Thread(() -> {
            System.out.println("Action thread: " + Thread.currentThread().getName());
            testController.someAction();
        }).start();
    }
}


class TestController {

    public String someAction() {
        // BEGIN onEnter Advice
        DebugTree tree = new DebugTree();
        tree.addMethodCall("someAction");
        // END onEnter Advice

        // Action logic
        ExampleService exampleService = new ExampleService();
        Thread serviceThread = new Thread(() -> {
            System.out.println("Service thread: " + Thread.currentThread().getName());
            exampleService.getUser();
        });

        serviceThread.start();
        try {
            serviceThread.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            Thread.currentThread().interrupt();
        }


        // BEGIN onExit Advice
        System.out.println("Action debug tree state after: " + tree.getTree());
        tree.state.remove();
        // END onExit Advice

        return ""; // response
    }
}

class ExampleService {

    void getUser() {
        System.out.println("ExampleService.getUser state before: " + DebugTree.getTree());
        DebugTree.addMethodCall("getUser");
        System.out.println("ExampleService.getUser state after: " + DebugTree.getTree());
        // we remove it after method call because we dont want it to be there after this task is completed and the thread returns to the pool
        DebugTree.state.remove();
    }
}

class DebugTree {

    public static ThreadLocal<ArrayList<String>> state = new InheritableThreadLocal<>();

    public static void addMethodCall(String methodName) {
        if (state.get() == null) { state.set(new ArrayList<>()); }
        state.get().add(methodName);
    }

    public static String getTree() {
        return state.get().toString();
    }
}
