package com.example;

import redis.clients.jedis.Jedis;

import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * https://redis.io/topics/distlock
 * Created by think on 17-4-23.
 */
public class RsLock {
    private static final String KEY_TMPL = "GUID_RSLOCK{8125CFBE-9237-4E0A-947C-CE99A1BD587E}{%s}";
    private final Jedis jedis;
    private final String key;
    private final int timeout;
    private final String identity;
    private static final Random random = new Random();
    private final int maxInterval;
    private long lockedTime = 0;

    public RsLock(Jedis jedis, String key, int timeout) {
        this(jedis, key, timeout, 100);
    }

    public RsLock(Jedis jedis, String key, int timeout, int maxInterval) {
        this.jedis = jedis;
        this.key = getLockKey(key);
        this.timeout = timeout;
        this.maxInterval = maxInterval;
        this.identity = UUID.randomUUID().toString();
    }

    private static String getLockKey(String key) {
        return String.format(KEY_TMPL, key);
    }



    public boolean lock() {
        String ok=jedis.set(key, identity,"nx","ex",timeout);
        if ("ok".equalsIgnoreCase(ok)) {
            if (lockedTime <= 0) {
                lockedTime = System.currentTimeMillis();
            }
            return true;
        }
        return false;
    }

    /**
     * @param sec seconds
     */
    public boolean tryLockWithinSeconds(int sec) {
       return tryLockWithin(sec*1000L);
    }
    /**
     * @param retryTimeout ms
     */
    public boolean tryLockWithin(long retryTimeout) {
        long a = System.currentTimeMillis() + retryTimeout;

        while (true) {
            if (lock()) {
                return true;
            } else {
                if (System.currentTimeMillis() < a) {
                    try {
                        Thread.sleep(random.nextInt(maxInterval));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
    }

    public void unlock() throws RsLockTimeoutException {
        String exist = jedis.get(key);
        if (identity.equalsIgnoreCase(exist)) {
            jedis.del(key);
        } else {
            System.out.println(exist+" >< "+identity);
            throw new RsLockTimeoutException(System.currentTimeMillis() - lockedTime, timeout);
        }
    }

    public static class RsLockTimeoutException extends Exception {
        private static final String MSG_TMPL = "RsLockTimeout:duration=%sms,timeout=%sms";

        RsLockTimeoutException(long duration, long timeout) {
            super(String.format(MSG_TMPL, duration, timeout * 1000));
        }
    }
}

