package com.demo;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.PersistentUserManagedCache;
import org.ehcache.UserManagedCache;
import org.ehcache.config.builders.*;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.PersistentUserManagedEhcache;
import org.ehcache.core.events.CacheEventListenerConfiguration;
import org.ehcache.core.spi.service.LocalPersistenceService;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.ehcache.event.EventType;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expirations;
import org.ehcache.impl.config.persistence.CacheManagerPersistenceConfiguration;
import org.ehcache.impl.config.persistence.DefaultPersistenceConfiguration;
import org.ehcache.impl.config.persistence.UserManagedPersistenceContext;
import org.ehcache.impl.persistence.DefaultLocalPersistenceService;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by lebron374 on 2017/6/27.
 */
public class EhcacheDemo {

    /**
     * 事件监听器，包括缓存内容的创建和过期
     */
    class MyCacheEventListener implements CacheEventListener {
        @Override
        public void onEvent(CacheEvent cacheEvent) {
            System.out.println(cacheEvent.getType().toString());
        }
    }

    CacheEventListenerConfigurationBuilder cacheEventListenerConfiguration =
            CacheEventListenerConfigurationBuilder.newEventListenerConfiguration(new MyCacheEventListener(),
                    new HashSet<EventType>(Arrays.asList(EventType.values())));
    //堆缓存
    CacheManager heapCacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true);
    CacheConfigurationBuilder<String, String> heapCacheConfig = CacheConfigurationBuilder.newCacheConfigurationBuilder(
            String.class,
            String.class,
            ResourcePoolsBuilder.newResourcePoolsBuilder().heap(10, EntryUnit.ENTRIES)).
            withDispatcherConcurrency(10).
            withExpiry(Expirations.timeToLiveExpiration(Duration.of(10, TimeUnit.SECONDS))).
            add(cacheEventListenerConfiguration);

    Cache<String, String> heapCache = heapCacheManager.createCache("heapCache", heapCacheConfig.build());

    //堆外缓存
    CacheManager offHeapCacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true);
    CacheConfigurationBuilder<String, String> offHeapCacheConfig = CacheConfigurationBuilder.newCacheConfigurationBuilder(
            String.class,
            String.class,
            ResourcePoolsBuilder.newResourcePoolsBuilder().offheap(10, MemoryUnit.MB)).
            withDispatcherConcurrency(10).
            withExpiry(Expirations.timeToLiveExpiration(Duration.of(10, TimeUnit.SECONDS))).
            add(cacheEventListenerConfiguration);
    Cache<String, String> offHeapCache = offHeapCacheManager.createCache("offHeapCache", offHeapCacheConfig.build());

    //磁盘缓存
    CacheManager diskCacheManager = CacheManagerBuilder.newCacheManagerBuilder().using(PooledExecutionServiceConfigurationBuilder.
            newPooledExecutionServiceConfigurationBuilder().defaultPool("default", 1, 10).build()).
            with(new CacheManagerPersistenceConfiguration(new File("D:\\temp.db"))).build(true);
    CacheConfigurationBuilder<String, String> diskCacheConfig = CacheConfigurationBuilder.newCacheConfigurationBuilder(
            String.class,
            String.class,
            ResourcePoolsBuilder.newResourcePoolsBuilder().disk(10, MemoryUnit.MB, true)).
            withDiskStoreThreadPool("default", 5).
            withExpiry(Expirations.timeToLiveExpiration(Duration.of(10, TimeUnit.MINUTES))).
            withSizeOfMaxObjectGraph(3).withSizeOfMaxObjectSize(1, MemoryUnit.KB).
            add(cacheEventListenerConfiguration);
    Cache<String,String> diskCache = diskCacheManager.createCache("diskCache", diskCacheConfig.build());

    //User managed cache 而非由CacheManager控制的
    UserManagedCache<String, String> userCache = UserManagedCacheBuilder.newUserManagedCacheBuilder(String.class, String.class).
            withExpiry(Expirations.timeToLiveExpiration(Duration.of(10, TimeUnit.SECONDS))).build(true);

    //User managed cache 包含持久化功能
    LocalPersistenceService persistenceService = new DefaultLocalPersistenceService(new DefaultPersistenceConfiguration(new File("D:\\UserData")));
    PersistentUserManagedCache<String, String> userPersistCache = UserManagedCacheBuilder.newUserManagedCacheBuilder(String.class, String.class).
            with(new UserManagedPersistenceContext<String, String>("uerPersistCache", persistenceService)).
            withResourcePools(ResourcePoolsBuilder.newResourcePoolsBuilder().heap(10, EntryUnit.ENTRIES).disk(10, MemoryUnit.MB, true)).
            build(true);

    /**
     * 测试堆缓存
     */
    public void test1() {

        for (int i=0; i<20; i++) {
            heapCache.put("key"+i, "value"+i);
        }

        for (int i=0; i <20; i++) {
            System.out.println(heapCache.get("key"+i));
        }
    }

    /**
     * 测试堆外缓存
     */
    public void test2() throws Exception{
        for (int i=0; i<20; i++) {
           offHeapCache.put("key"+i, "value"+i);
        }

        for (int i=0; i <20; i++) {
            System.out.println(offHeapCache.get("key"+i));
        }

        Thread.sleep(15000);

        for (int i=0; i <20; i++) {
            System.out.println(offHeapCache.get("key"+i));
        }
    }

    /**
     * 测试磁盘缓存
     */
    public void test3() throws Exception {
        for (int i=0; i<20; i++) {
            diskCache.put("key"+i, "value"+i);
        }

        for (int i=0; i <20; i++) {
            System.out.println(diskCache.get("key"+i));
        }

        Thread.sleep(15000);

        for (int i=0; i <20; i++) {
            System.out.println(diskCache.get("key"+i));
        }

        diskCacheManager.close();
    }

    /**
     * 测试内存版本的user manage cache
     */
    public void test4() throws Exception{
        for (int i=0; i<20; i++) {
            userCache.put("key"+i, "value"+i);
        }

        for (int i=0; i <20; i++) {
            System.out.println(userCache.get("key"+i));
        }

        Thread.sleep(16000);

        for (int i=0; i <20; i++) {
            System.out.println(userCache.get("key"+i));
        }

        userCache.close();
    }

    /**
     * 测试持久化版本的user manage cache
     */
    public void test5() throws Exception{
        for (int i=0; i<20; i++) {
            userPersistCache.put("key"+i, "value"+i);
        }

        for (int i=0; i <20; i++) {
            System.out.println(userPersistCache.get("key"+i));
        }

        Thread.sleep(17000);

        for (int i=0; i <20; i++) {
            System.out.println(userPersistCache.get("key"+i));
        }

        persistenceService.stop();
        userPersistCache.close();
    }

    public static void main(String[] args) throws Exception {
        EhcacheDemo demo = new EhcacheDemo();

//        demo.test1();
//        demo.test2();
        demo.test3();
//        demo.test4();
//        demo.test5();
    }
}
