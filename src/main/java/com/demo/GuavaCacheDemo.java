package com.demo;

import com.google.common.cache.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;

/**
 * Created by lebron374 on 2017/6/27.
 */
public class GuavaCacheDemo {

    // 基于内存的缓存，定义了未命中缓存获取数据的方法
    private final LoadingCache<String, String> loadingCache = CacheBuilder.newBuilder().
            initialCapacity(5).
            concurrencyLevel(5).
            expireAfterWrite(5, TimeUnit.SECONDS).
            weakKeys().
            maximumSize(5).
            recordStats().
            removalListener(new RemovalListener<String, String>() {
                @Override
                public void onRemoval(RemovalNotification<String, String> notification) {
                    printTime();
                    System.out.println(notification.getKey() + " has been remove");
                }
            }).
            build(new CacheLoader<String, String>() {
                @Override
                public String load(String key) throws Exception {
                    System.out.println(key + " generate new value");
                    return key + "_Cache_Value";
                }
            });

    private final Cache<String, String> cache = CacheBuilder.newBuilder().
            initialCapacity(5).
            concurrencyLevel(5).
            expireAfterWrite(5, TimeUnit.SECONDS).
            weakKeys().
            maximumSize(5).
            recordStats().removalListener(new RemovalListener<String, String>() {
                @Override
                public void onRemoval(RemovalNotification<String, String> notification) {
                    System.out.println(notification.getKey() + " has been remove");
                }
            }).build();

    /**
     * 测试获取缓存
     * 第一次由于key2不存在所以会调用CacheLoader加载key2对应的value
     * 第二次由于key2已然存在所以直接取缓存的keys
     */
    public void test1() {
        try {
            System.out.println(loadingCache.get("key2"));
            System.out.println(loadingCache.get("key2"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试移除缓存
      */
    public void test2() {
        try {
            System.out.println(loadingCache.get("key2"));
            System.out.println(loadingCache.get("key2"));
            loadingCache.invalidate("key2");
            printTime();
            System.out.println(loadingCache.get("key2"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试基于容量的缓存过期策略,缓存数量设置为上限5个
     */
    public void test3() {
        for (int i=0; i<10; i++) {
            loadingCache.put("key" + i, "value3");
        }
    }

    /**
     * 测试Callable接口
     * @throws Exception
     */
    public void test4() throws Exception{

        cache.get("key4", new Callable<String>() {
            @Override
            public String call() throws Exception {
                System.out.println("key4 generate new value");

                return "key4_Cache_Value";
            }
        });

        System.out.println(cache.getIfPresent("key4"));
        System.out.println(cache.getIfPresent("key3"));
    }

    /**
     * 测试数据过期手工清理
     */
    public void test5() throws Exception{
        printTime();
        loadingCache.put("key3", "value3");
        System.out.println(loadingCache.get("key3"));
        Thread.sleep(5000);
        loadingCache.cleanUp();
        Thread.sleep(5000);
        printTime();
        System.out.println(loadingCache.get("key4"));
    }

    /**
     * 打印时间格式
     */
    public void printTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println(dateFormat.format(new Date()));
    }

    /**
     * LoadCache的统计数据
     */
    public void loadingCacheStats() {
        System.out.println(loadingCache.stats());
    }

    public void cacheStats() {
        System.out.println(cache.stats());
    }

    public static void main (String[] args) throws Exception{

        GuavaCacheDemo demo = new GuavaCacheDemo();
        demo.test5();
        demo.cacheStats();
    }

}
