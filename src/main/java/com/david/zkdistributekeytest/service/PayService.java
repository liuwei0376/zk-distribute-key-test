package com.david.zkdistributekeytest.service;

import com.david.zkdistributekeytest.util.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PayService {

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private ItemsService itemsService;

    @Autowired
    private DistributedLock distributedLock;

    public boolean buy(String itemId) {

        //线程一进来先加ZK分布式锁，锁定销售资源
        distributedLock.getLock();

        /**
         * 一次下单购买9件
         */
        int buyCount = 9;

        //1. 查库存(库存是否可以支持此次购买)
        int count = itemsService.getItemCounts(itemId);

        if (count < buyCount) { //库存不够
            log.error("当前库存不足，下单失败。购买商品数：{}，实际库存：{}", buyCount, count);

            distributedLock.tryReleaseLock();
            return false; //无法支付
        }

        //2. 创建订单
        boolean flag = false;

        try {
            flag = ordersService.save(itemId);
            Thread.sleep(5*1000);
        } catch (InterruptedException e) {
            distributedLock.tryReleaseLock();

            e.printStackTrace();
        }

        //3. 扣库存
        if (flag) {
            itemsService.reduceCounts(itemId, buyCount);

            distributedLock.tryReleaseLock();
        } else {
            log.error("订单创建失败。。。。。。");
            distributedLock.tryReleaseLock();

            return false;
        }

        return true;
    }

}
