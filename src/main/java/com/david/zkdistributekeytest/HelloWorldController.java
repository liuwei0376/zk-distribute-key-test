package com.david.zkdistributekeytest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {

    @RequestMapping("/hello")
    public String sayHelloWorld(){
        return "Hello david";
    }

}
