package com.jiyuan;

import com.jiyuan.service.UserService;
import com.spring.JiyuanApplicationContext;

public class Test {

    public static void main(String[] args) {

        JiyuanApplicationContext applicationContext = new JiyuanApplicationContext(AppConfig.class);

        UserInterface userService = (UserInterface) applicationContext.getBean("userService");
        userService.test();
    }
}
