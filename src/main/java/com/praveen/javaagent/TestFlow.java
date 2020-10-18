//package com.praveen.javaagent;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ThreadFactory;
//
//public class TestFlow {
//
//    private static ExecutorService pool  = Executors.newFixedThreadPool(1);
//
//
//    private static ThreadLocal<String> tl = new InheritableThreadLocal<>();
//
//    public static void main(String[] args) throws InterruptedException {
//        pool.submit(() -> true);
//        pool.submit(() -> true);
//        pool.submit(() -> true);
//        pool.submit(() -> true);
//        pool.submit(() -> true);
//
//        System.out.println(Thread.currentThread().getId());
//        // instrument anything that implements runnable onEnter copy from current threadlocal into here onExit remove context
//        //
//        new Thread(() -> {
//            tl.set("main");
//            test("expect 'main' because current");
//
//            pool.submit(() -> {
//                test("expect null because parent does not have value set?");
//            });
//
//
//        }).start();
//
//
//        Thread.sleep(1000);
//        pool.shutdown();
//    }
//
//    private static void test(String label) {
//        long id = Thread.currentThread().getId();
//        System.out.println("thread=" + id + " " + label + " " + tl.get());
//    }
//}
//
//
//class TestController {
//
//    public String someAction() {
//        // BEGIN onEnter Advice
//        DebugTree tree = new DebugTree();
//        tree.addMethodCall("someAction");
//        // END onEnter Advice
//
//        // Action logic
//        ExampleService exampleService = new ExampleService();
//        Thread serviceThread = new Thread(() -> {
//            System.out.println("Service thread: " + Thread.currentThread().getName());
//            exampleService.getUser();
//        });
//
//        Thread serviceThread1 = new Thread(() -> {
//            System.out.println("Service thread: " + Thread.currentThread().getName());
//            exampleService.getUser();
//        });
//
//        serviceThread.start();
//        serviceThread1.start();
//        try {
//            serviceThread.join();
//            serviceThread1.join();
//        } catch (InterruptedException ex) {
//            ex.printStackTrace();
//            Thread.currentThread().interrupt();
//        }
//
//
//        // BEGIN onExit Advice
//        System.out.println("Action debug tree state after: " + tree.getTree());
//        tree.state.remove();
//        // END onExit Advice
//
//        return ""; // response
//    }
//}
//
//class ExampleService {
//
//    void getUser() {
//        System.out.println("ExampleService.getUser state before: " + DebugTree.getTree());
//        DebugTree.addMethodCall("getUser");
//        System.out.println("ExampleService.getUser state after: " + DebugTree.getTree());
//        // we remove it after method call because we dont want it to be there after this task is completed and the thread returns to the pool
//        DebugTree.state.remove();
//    }
//}
//
//class DebugTree {
//
//    public static ThreadLocal<HashMap<String, ArrayList<String>>> state = new InheritableThreadLocal<>();
//
//    public static void addMethodCall(String methodName) {
//        if (state.get() == null) { state.set(new HashMap<String, ArrayList<String>>()); }
//
//        if (state.get().get(Thread.currentThread().getName()) == null) {
//            state.get().put(Thread.currentThread().getName(), new ArrayList<String>(Arrays.asList(methodName)));
//        } else {
//            state.get().get(Thread.currentThread().getName()).add(methodName);
//        }
//    }
//
//    public static String getTree() {
//        return state.get().toString();
//    }
//}
//
//// Maybe better to do Map["methodName", MethodMetadata] when MethodMetadata has
