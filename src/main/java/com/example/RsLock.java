package com.example;

import redis.clients.jedis.Jedis;

import java.util.Random;
import java.util.UUID;

/**
 * https://redis.io/topics/distlock
 * Created by think on 17-4-23.
 */
public class RsLock {
    private static final String KEY_TMPL = "GUID_RSLOCK{8125CFBE-9237-4E0A-947C-CE99A1BD587E}{%s}";
    private static final String GUARD_KEY_TMPL = "GUID_RSLOCK_GUARD{8125CFBE-9237-4E0A-947C-CE99A1BD587E}{%s}";
    private final Jedis jedis;
    private final String key;
    private final String guardKey;
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
        this.guardKey = getGuardKey(key);
        this.timeout = timeout;
        this.maxInterval = maxInterval;
        this.identity = UUID.randomUUID().toString();
    }

    private static String getLockKey(String key) {
        return String.format(KEY_TMPL, key);
    }

    private static String getGuardKey(String key) {
        return String.format(GUARD_KEY_TMPL, key);
    }

    public boolean lock() {
        if (lockNX()) {
            return true;
        } else {
            String guard = jedis.get(guardKey);
            if (null == guard) {
                jedis.del(key);
                return lockNX();
            } else {
                return false;
            }
        }
    }

    private boolean lockNX() {
        Long setnx = jedis.setnx(key, identity);
        if (setnx == 1) {
            Long expire = jedis.expire(key, timeout);
            String setex = jedis.setex(guardKey, timeout, identity);
            if (expire == 1 && "ok".equalsIgnoreCase(setex)) {
                if (lockedTime <= 0) {
                    lockedTime = System.currentTimeMillis();
                }
                return true;
            }
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
            jedis.del(key, guardKey);
        } else {
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

