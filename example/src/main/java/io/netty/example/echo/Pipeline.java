package io.netty.example.echo;

import java.util.concurrent.atomic.AtomicInteger;

public class Pipeline {
    public static void main(String[] args) {
        long lTime = System.currentTimeMillis();
//        PowerOfTwo();
        Generic();
        System.out.println(System.currentTimeMillis()-lTime);
    }
    private static void Generic() {
        AtomicInteger idx = new AtomicInteger();
        int[] num = new int[32];
        for (idx.get(); idx.get() < 100000000; ) {
//            System.out.print(idx.get() + ":");
//            System.out.println(Math.abs(idx.getAndIncrement() % num.length));
            int i = Math.abs(idx.getAndIncrement() % num.length);
        }
    }
    private static void PowerOfTwo() {
        AtomicInteger idx = new AtomicInteger();
        int[] num = new int[32];
        for (idx.get(); idx.get() < 100000000; ) {
//            System.out.print(idx.get() + ":");
//            System.out.println(idx.getAndIncrement() & num.length-1);
            int i = idx.getAndIncrement() & num.length - 1;
        }
    }
}
