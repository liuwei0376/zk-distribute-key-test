package com.david.zkdistributekeytest.controller;

import com.david.zkdistributekeytest.service.PayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Slf4j
public class PayController {

    @Autowired
    private PayService payService;

    @GetMapping("/buy1")
    @ResponseBody
    public String buy1(@RequestParam String itemId) {
        log.info("当前线程：{}, 传参过来的 itemId = {} " ,Thread.currentThread().getName() , itemId);

        boolean flag = payService.buy(itemId);

        if (!StringUtils.isEmpty(itemId)) {
            if (flag) {
                return "订单创建成功";
            } else {
                return "订单创建失败";
            }
        } else {
            return "itemId 传参不能为空";
        }
    }

    @GetMapping("/buy2")
    @ResponseBody
    public String buy2(@RequestParam String itemId) {
        log.info("当前线程：{}, 传参过来的 itemId = {} " ,Thread.currentThread().getName() , itemId);

        boolean flag = payService.buy(itemId);

        if (!StringUtils.isEmpty(itemId)) {
            if (flag) {
                return "订单创建成功";
            } else {
                return "订单创建失败";
            }
        } else {
            return "itemId 传参不能为空";
        }
    }

}
