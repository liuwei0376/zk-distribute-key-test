package com.david.zkdistributekeytest.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.CountDownLatch;


@Configuration //为了纳入spring的管理，同时用以在service中使用该类 。 等价于spring的xml配置文件中的<beans>，为后续管理<bean>而创建。
@Slf4j
public class DistributedLock {

    private CuratorFramework client = null;

    private static final String ZK_LOCKS = "david-zk-locks";
    private static final String DISTRIBUTED_LOCK = "david-distributed-lock";

    private static CountDownLatch countDownLatch = new CountDownLatch(1);

    public DistributedLock() { //构造器先于 以下@Bean的初始化之前执行
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 1");

        client = CuratorFrameworkFactory.builder()
                .connectString("localhost:2181")
                .sessionTimeoutMs(10 * 1000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .namespace("zk-namespace")
                .build();

        client.start();
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 2");
    }

    @Bean //容器启动时，执行该方法 。@Bean标注在方法上(返回某个实例的方法)，等价于spring的xml配置文件中的<bean>，作用为：注册bean对象
    public CuratorFramework initClient() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 3");
        client = client.usingNamespace("zk-namespace");

        try {
            if (client.checkExists().forPath("/" + ZK_LOCKS) == null) {
                client.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            addWatcher("/" + ZK_LOCKS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return client;
    }

    //设置节点异动监听事件
    private void addWatcher(String path) throws Exception {
        PathChildrenCache cache = new PathChildrenCache(client, path, true);

        cache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);

        cache.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                if (event.getType().equals(PathChildrenCacheEvent.Type.INITIALIZED)) {
                    System.out.println(String.format("子节点 %s 初始化成功", event.getData() == null ? "~nUll~" : event.getData().getPath()));
                } else if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_ADDED)) {
                    System.out.println("监控到：\n添加子节点路径 : " + event.getData().getPath());
                    System.out.println("子节点数据为 : " + new String(event.getData().getData(), "UTF-8"));
                } else if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_REMOVED)) {
                    System.out.println("监控到：\n移除子节点路径 : " + event.getData().getPath());
                    System.out.println("子节点数据为 : " + new String(event.getData().getData(), "UTF-8"));

                    String path = event.getData().getPath();

                    //如果 DISTRIBUTED_LOCK 节点被删除，说明上个事务已成功执行，并释放zk锁。这时计数器应该减一。
                    if (path.contains(DISTRIBUTED_LOCK)) {
                        log.error(">> addWatcher方法中（type: CHILD_REMOVED）：当前线程：{},  countDown()方法调用前： countDownLatch.getCount() = {} ", Thread.currentThread().getName(), countDownLatch.getCount());
                        countDownLatch.countDown();
                        log.error(">> addWatcher方法中（type: CHILD_REMOVED）：当前线程：{},  countDown()方法调用后： countDownLatch.getCount() = {} ", Thread.currentThread().getName(), countDownLatch.getCount());
                    }

                } else if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_UPDATED)) {
                    System.out.println("监控到：\n更新子节点路径 : " + event.getData().getPath());
                    System.out.println("子节点数据为 : " + new String(event.getData().getData(), "UTF-8"));
                }
            }
        });
    }

    public void getLock() {

        int retryTimes = 0;

        while (true) {
            try {
                log.info(">> 正在尝试进行第 {} 次获取zk锁", ++retryTimes);
                client.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL)
                        .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                        .forPath("/" + ZK_LOCKS + "/" + DISTRIBUTED_LOCK);

                log.info("分布式zk锁获取成功， 位置1：当前countDownLatch.getCount() = {}", countDownLatch.getCount());
            } catch (Exception e) {
                e.printStackTrace();

                //如果获取zk锁失败，因之前执行了countDown的减一操作，这里要还原回去。
                log.error(">> getLock方法中：当前线程：{},  countDownLatch.getCount() = {} ", Thread.currentThread().getName(), countDownLatch.getCount());

                if (countDownLatch.getCount() <= 0) {
                    countDownLatch = new CountDownLatch(1);
                }
                //同时要让其处于等待状态
                try {
                    countDownLatch.await();
                    log.info("》》》》countDownLatch.await() 的等待状态结束，将执行main流程业务");
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }

            log.info("分布式zk锁获取成功， 位置2：当前countDownLatch.getCount() = {}", countDownLatch.getCount());

            return;
        }
    }

    //释放分布式zk锁，在订单创建成功、异常的时候释放锁。
    public boolean tryReleaseLock() {
        System.out.println("调用了tryReleaseLock方法，在释放锁之前，先删除zk上临时节点: " + "/" + ZK_LOCKS + "/" + DISTRIBUTED_LOCK);
        try {
            if (client.checkExists().forPath("/" + ZK_LOCKS + "/" + DISTRIBUTED_LOCK) != null) {
                client.delete().forPath("/" + ZK_LOCKS + "/" + DISTRIBUTED_LOCK);
            }
        } catch (Exception e) {
            e.printStackTrace();

            return false;
        }
        System.out.println("分布式zk锁释放成功");
        return true;
    }
}
