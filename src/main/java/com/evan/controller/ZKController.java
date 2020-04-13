package com.evan.controller;

import com.evan.utils.ZKLock;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author evanYang
 * @version 1.0
 * @date 2020/04/12 22:35
 */
@RestController
@RequestMapping("/zk")
public class ZKController {
    @Autowired
    private CuratorFramework zkClient;
    private String url = "127.0.0.1:2181";
    private int timeout = 3000;
    private String lockPath = "/testl";
    @Autowired
    private ZKLock zklock;
    private int k = 1;

    @GetMapping("/lock")
    public Boolean getLock() throws Exception {
        for (int i = 0; i < 10; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    zklock.lock();
                    zklock.unlock();
                }
            }).start();
        }
        return true;
    }

}
