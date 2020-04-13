package com.evan.utils;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @author evanYang
 * @version 1.0
 * @date 2020/04/12 22:02
 */
@Component("zkLock")
public class ZKLock implements Lock {
    @Autowired
    private CuratorFramework zkClient;
    @Value("${zk.localPath}")
    private String lockPath;
    private String currentPath;
    private String beforePath;

    @Override
    public void lock() {
        if (!tryLock()) {
            waiForLock();
            lock();
        }
    }

    @Override
    public void unlock() {
        try {
            zkClient.delete().guaranteed().deletingChildrenIfNeeded().forPath(currentPath);
        } catch (Exception e) {
            //guaranteed()保障机制，若未删除成功，只要会话有效会在后台一直尝试删除
        }
    }

    @Override
    public boolean tryLock() {
        return false;
    }
    private void waiForLock(){
        CountDownLatch downLatch = new CountDownLatch(1);
        //创建节点监听器
        NodeCache nodeCache = new NodeCache(zkClient,beforePath);
        try {
            nodeCache.start(true);
            //https://www.cnblogs.com/domi22/p/9748083.html
            nodeCache.getListenable().addListener(new NodeCacheListener() {
                @Override
                public void nodeChanged() throws Exception {
                    downLatch.countDown();
                    System.out.println(beforePath + "节点监听事件触发，重新获得节点内容为：" + new String(nodeCache.getCurrentData().getData()));
                }
            });
        }catch (Exception e){

        }

        //如果前一个节点还存在，则阻塞自己
        try {
            if (zkClient.checkExists().forPath(beforePath) == null) {
                downLatch.await();
            }
        } catch (Exception e) {
        }finally {
            //阻塞结束，说明自己是最小的节点，则取消watch，开始获取锁
            try {
                nodeCache.close();
            } catch (IOException e) {
            }
        }

    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }



    @Override
    public Condition newCondition() {
        return null;
    }
    @Override
    public void lockInterruptibly() throws InterruptedException {

    }
}
