package com.stratocloud.cache.local;

import com.stratocloud.cache.CacheLock;
import org.junit.Test;

import java.util.concurrent.*;

public class TestLocalLock {


    @Test
    public void test(){
        LocalCacheService cacheService = new LocalCacheService();

        CacheLock lock = cacheService.getLock("123");

        assert lock.tryLock(10);
        assert !lock.tryLock(10);

        assert lock.tryLock(12, 10);
        assert lock.tryLock(12, 10);

        assert !cacheService.getLock("123").tryLock(10);
        assert cacheService.getLock("456").tryLock(10);

        cacheService.shutDown();
    }

    @Test
    public void test2() throws Exception {
        LocalCacheService cacheService = new LocalCacheService();

        Callable<Boolean> callable = () -> cacheService.getLock("789").tryLock(10);

        FutureTask<Boolean> task1 = new FutureTask<>(callable);
        FutureTask<Boolean> task2 = new FutureTask<>(callable);

        new Thread(task1).start();
        new Thread(task2).start();

        assert task1.get() || task2.get();
        assert !(task1.get() && task2.get());

        cacheService.shutDown();
    }



}
