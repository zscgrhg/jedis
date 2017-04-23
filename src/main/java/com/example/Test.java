package com.example;

import redis.clients.jedis.Jedis;

import java.util.Random;

/**
 * Created by think on 17-4-23.
 */
public class Test {
    static Jedis jedis = new Jedis("localhost");

    public static void tryLock(String key, int sec) {
        Long setnx = jedis.setnx(key, String.valueOf(1));
        if (1 == setnx) {
            jedis.expire(key, sec);
        }
    }

    public static void main(String[] args) throws InterruptedException {

        new Thread(new Runnable() {
            public void run() {
                Jedis jedis = new Jedis("localhost");
                Random random = new Random();
                while (!Thread.currentThread().isInterrupted()) {
                    String oops = RLock.getLockKey("OOPS");

                    if(random.nextBoolean()
                            &&random.nextBoolean()
                            &&random.nextBoolean()){
                        jedis.set(oops, "DeadLock!");
                    }
                    System.out.println(">"+jedis.get(oops));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }).start();
        for (int i = 0; i < 100; i++) {
            RLock rLock = new RLock(jedis, "OOPS", 3);
            if (rLock.tryLock(10000)) {
                System.out.println("lock oops success!");
                if (i % 5 > 1) {
                    Thread.sleep(200);
                    try {
                        rLock.unlock();
                    } catch (RLock.RLockTimeoutException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                System.out.println("    lock oops failed!");
            }
            Thread.sleep(1000);
        }
    }
}
