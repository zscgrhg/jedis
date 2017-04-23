package com.example;

import redis.clients.jedis.Jedis;

import java.util.Random;

/**
 * Created by think on 17-4-23.
 */
public class Usage {
    public static final String test_key = "OOPS";

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 200; i++) {

            new Thread(new Runnable() {
                Jedis jedis = new Jedis("localhost");
                long id ;
                public void run() {
                    id = Thread.currentThread().getId();
                    for (int j = 0; j < 10000; j++) {
                        try {
                            Thread.sleep(new Random().nextInt(1000));
                        } catch (InterruptedException e) {

                        }
                        RsLock rsLock = new RsLock(jedis, test_key, 10);
                        boolean b = rsLock.tryLockWithinSeconds(10);
                        if (b) {
                            jedis.set(test_key,
                                    String.valueOf(id));
                            try {
                                rsLock.unlock();
                            } catch (RsLock.RsLockTimeoutException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }).start();
        }
        Jedis jedis = new Jedis("localhost");
        while (true) {
            System.out.println(jedis.get(test_key));
            Thread.sleep(100L);
        }
    }
}
