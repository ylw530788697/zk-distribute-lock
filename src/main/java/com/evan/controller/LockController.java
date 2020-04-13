package com.evan.controller;

import com.evan.utils.ZkClientHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class LockController {

    private static final Logger logger = LoggerFactory.getLogger(LockController.class);

    @Autowired
    private ZkClientHelper zkClientHelper;

    @Value("${spring.curator.interface-server.share-node}")
    String shareNode;

    @RequestMapping("lock1")
    public void lock1() {
        zkClientHelper.acquireDistributedLock("ROOT-LOCK",shareNode);
        try {
            logger.info("I am lock1，i am updating resource……！！！");
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            zkClientHelper.releaseDistributedLock("ROOT-LOCK",shareNode);
        }
    }

    @RequestMapping("lock2")
    public void lock2() {
        zkClientHelper.acquireDistributedLock("ROOT-LOCK",shareNode);
        try {
            logger.info("I am lock2，i am updating resource……！！！");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            zkClientHelper.releaseDistributedLock("ROOT-LOCK",shareNode);
        }
    }


    @RequestMapping("lock3")
    public void lock3() {
        zkClientHelper.acquireDistributedLock("ROOT-LOCK",shareNode);
        try {
            logger.info("I am lock3，i am updating resource……！！！");
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            zkClientHelper.releaseDistributedLock("ROOT-LOCK",shareNode);
        }
    }

    @RequestMapping("lock4")
    public void lock4() {
        zkClientHelper.acquireDistributedLock("ROOT-LOCK",shareNode);
        try {
            logger.info("I am lock4，i am updating resource……！！！");
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            zkClientHelper.releaseDistributedLock("ROOT-LOCK",shareNode);
        }
    }
}
