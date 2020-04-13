package com.evan.utils;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.CountDownLatch;

@Component
@ConfigurationProperties(prefix = "spring.curator")
public class ZkClientHelper implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(ZkClientHelper.class);
    private static CountDownLatch countDownLatch = new CountDownLatch(1);

    // 失败重试间隔时间 单位:毫秒
    private int sleepTimeMs;
    // 失败重试次数
    private int maxRetries;
    // 会话存活时间 单位:毫秒
    private int sessionTimeOutMs;
    // zookeeper 服务地址
    private String zookeeperAddress;

    private List<String> lockNode;

    public void setSleepTimeMs(int sleepTimeMs) {
        this.sleepTimeMs = sleepTimeMs;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public void setSessionTimeOutMs(int sessionTimeOutMs) {
        this.sessionTimeOutMs = sessionTimeOutMs;
    }

    public void setZookeeperAddress(String zookeeperAddress) {
        this.zookeeperAddress = zookeeperAddress;
    }

    public List<String> getLockNode() {
        return lockNode;
    }

    public void setLockNode(List<String> lockNode) {
        this.lockNode = lockNode;
    }

    @Bean
    @Order(-1)
    CuratorFramework build(){
        CuratorFramework client = CuratorFrameworkFactory
                .builder()
                .connectString(zookeeperAddress)
                .retryPolicy(new ExponentialBackoffRetry(sleepTimeMs,maxRetries))
                .namespace(ZK_CONSTANT.NAMESPACE.getValue())
                .sessionTimeoutMs(sessionTimeOutMs)
                .build();
        client.start();
        return client;
    }

    @Override
    public void run(String... args) throws Exception {
        CuratorFramework curatorFramework = build();
        curatorFramework.usingNamespace("zookeeper-lock");
        lockNode.forEach(node -> {
            String lockPath = ZK_CONSTANT.SPLIT.getValue() + node;
            // 检查锁节点是否存在
            Stat stat = null;
            try {
                stat = curatorFramework.checkExists()
                        .forPath(lockPath);
            if (StringUtils.isEmpty(stat)) {
                // 若不存在锁节点，则创建锁节点
                curatorFramework.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                        .forPath(lockPath);
            }
            // 实现zookeeper 节点监听事件
            setWatcher(lockPath);
            logger.error("锁节点:{}初始化创建完成",lockPath);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("锁节点:{}初始化创建异常:{}",lockPath,e.getMessage());
            }
        });
    }

    private void setWatcher(String path) throws Exception {
        final PathChildrenCache cache = new PathChildrenCache(build(), path, false);
        cache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
        cache.getListenable().addListener((client, event) -> {
            if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_REMOVED)) {
                String oldPath = event.getData().getPath();
                logger.error("success to release lock for path:{}" , oldPath);
                if (oldPath.contains(path)) {
                    //释放计数器，让当前的请求获取锁
                    countDownLatch.countDown();
                }
            }
        });
    }

    /**
     *  获取锁
     *
     * @param rootPath 锁节点
     * @param path 节点
     */
    public void acquireDistributedLock(String rootPath,String path) {
        String lockPath = ZK_CONSTANT.SPLIT.getValue() + rootPath + ZK_CONSTANT.SPLIT.getValue() + path;
        while (true) {
            try {
                build()
                        .create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL)
                        .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                        .forPath(lockPath);
                break;
            } catch (Exception e) {
                logger.error("failed to acquire lock for path:{},exception:{}", lockPath,e.getMessage());
                try {
                    if (countDownLatch.getCount() <= 0) {
                        countDownLatch = new CountDownLatch(1);
                    }
                    countDownLatch.await();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     *  释放锁
     *
     * @param rootPath 锁节点
     * @param path 节点
     * @return
     */
    public void releaseDistributedLock(String rootPath,String path){
        String lockPath = ZK_CONSTANT.SPLIT.getValue() + rootPath + ZK_CONSTANT.SPLIT.getValue() + path;
        try {
            if (null != build().checkExists().forPath(lockPath)) {
                build().delete().forPath(lockPath);
            }
        } catch (Exception e) {
            logger.error("failed to release lock:{},exception:{}",lockPath,e.getMessage());
            try {
                Thread.sleep(3000);
                this.releaseDistributedLock(rootPath,path);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }
    }

    enum ZK_CONSTANT{
        SPLIT("/","分隔符"),NAMESPACE("SHARE_LOCK","锁域空间");
        String value ;
        String desc ;

        ZK_CONSTANT(String value, String desc) {
            this.value = value;
            this.desc = desc;
        }

        public String getValue() {
            return value;
        }

        public String getDesc() {
            return desc;
        }
    }
}
