package com.david.zkdistributekeytest.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;

import javax.transaction.Transactional;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PayServiceTest {

    @Autowired
    private PayService payService;

    @Test
    @Transactional
    @Rollback(value = false) //https://www.cnblogs.com/Demonfeatuing/p/9719184.html
    public void testBuy(){
        payService.buy("1");
    }

}
