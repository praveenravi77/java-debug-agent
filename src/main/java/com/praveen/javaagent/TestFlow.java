package com.praveen.javaagent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class TestFlow {

    private static ExecutorService pool  = Executors.newFixedThreadPool(1);


    private static ThreadLocal<String> tl = new InheritableThreadLocal<>();

    public static void main(String[] args) throws InterruptedException {
        pool.submit(() -> true);

        System.out.println(Thread.currentThread().getId());
        // instrument anything that implements runnable onEnter copy from current threadlocal into here onExit remove context
        //
        new Thread(() -> {
            tl.set("main");
            test("expect 'main' because current");

            pool.submit(() -> {
                test("expect null because parent does not have value set?");
            });


        }).start();

        Thread.sleep(1000);
        pool.shutdown();
    }

    private static void test(String label) {
        long id = Thread.currentThread().getId();
        System.out.println("thread=" + id + " " + label + " " + tl.get());
    }
}
