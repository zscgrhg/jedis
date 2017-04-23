package com.example;

import redis.clients.jedis.Jedis;

import java.util.Random;
import java.util.UUID;

/**
 * https://redis.io/topics/distlock
 * Created by think on 17-4-23.
 */
public class RsLock {
    private static final String KEY_TMPL = "GUID_RSLOCK{8125CFBE-9237-4E0A-947C-CE99A1BD587E}_%s";
    private static final String WATCHER_KEY_TMPL = "GUID_RSLOCK_WATCHER{8125CFBE-9237-4E0A-947C-CE99A1BD587E}_%s";
    private final Jedis jedis;
    private final String key;
    private final String watcherKey;
    private final int timeout;
    private final String identity;
    private static final Random random = new Random();
    private  final int maxInterval;

    public RsLock(Jedis jedis, String key, int timeout) {
        this(jedis,key,timeout,100);
    }
    public RsLock(Jedis jedis, String key, int timeout, int maxInterval) {
        this.jedis = jedis;
        this.key = getLockKey(key);
        this.watcherKey = getWatcherKey(key);
        this.timeout = timeout;
        this.maxInterval = maxInterval;
        this.identity = UUID.randomUUID().toString();
    }

    public static String getLockKey(String key) {
        return String.format(KEY_TMPL, key);
    }

    public static String getWatcherKey(String key) {
        return String.format(WATCHER_KEY_TMPL, key);
    }

    public boolean lock() {
        if (lockNX()) {
            return true;
        } else {
            String watcher = jedis.get(watcherKey);
            if (null == watcher) {
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
            jedis.expire(key, timeout);
            jedis.setex(watcherKey, timeout, identity);
            return true;
        }
        return false;
    }

    public boolean tryLock(long timeout) {
        long a = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < a) {
            if (lock()) {
                return true;
            } else {
                try {
                    Thread.sleep(random.nextInt(maxInterval));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    public void unlock() throws RLockTimeoutException {
        String exist = jedis.get(key);
        if (identity.equalsIgnoreCase(exist)) {
            jedis.del(key, watcherKey);
        } else {
            throw new RLockTimeoutException();
        }
    }

    public static class RLockTimeoutException extends Exception {

    }
}

