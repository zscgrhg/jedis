package com.example;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;


/**
 * Created by think on 17-4-23.
 */
public class RsLockTest {
    Jedis jedis;
    @Before
    public void setUp() throws Exception {
        jedis=new Jedis("localhost");
    }

    @Test
    public void testTryLockSuccess() throws InterruptedException {
        RsLock rsLock = new RsLock(jedis, "testTryLockSuccess", 4);
        if(rsLock.lock()){
            RsLock rsLock2 = new RsLock(jedis, "testTryLockSuccess", 4);
            boolean b = rsLock2.tryLock(5000);
            assert b;
        }
    }
    @Test
    public void testTryLockFailed() throws InterruptedException {
        RsLock rsLock = new RsLock(jedis, "testTryLockFailed", 4);

        if(rsLock.lock()){
            RsLock rsLock2 = new RsLock(jedis, "testTryLockFailed", 4);
            boolean b = rsLock2.tryLock(3000);
            assert !b;
        }
    }

    @Test(expected = RsLock.RsLockTimeoutException.class)
    public void testUnlock() throws InterruptedException, RsLock.RsLockTimeoutException {
        RsLock rsLock = new RsLock(jedis, "testUnlock", 1);
        if(rsLock.tryLock(10000)){
            Thread.sleep(2000);
            rsLock.unlock();
        }
    }

    @After
    public void tearDown() throws Exception {
        jedis.close();
    }
}
