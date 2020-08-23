package com.david.zkdistributekeytest.service;

import com.david.zkdistributekeytest.dao.OrdersDAO;
import com.david.zkdistributekeytest.domain.Orders;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class OrdersService {

    @Autowired
    private OrdersDAO ordersDAO;


    public boolean save(String itemId){
        try {
            Orders orders = new Orders();
            orders.setId(UUID.randomUUID().toString());
            orders.setItemId(itemId);

            ordersDAO.save(orders);

            log.info("订单创建成功。。。");

            return true;
        } catch (Exception e) {
            e.printStackTrace();

            return false;
        }
    }
}
