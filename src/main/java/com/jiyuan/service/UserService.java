package com.jiyuan.service;

import com.jiyuan.UserInterface;
import com.spring.BeanNameAware;
import com.spring.InitializingBean;
import com.spring.annotation.Autowired;
import com.spring.annotation.Component;
import com.spring.annotation.JiyuanValue;
import com.spring.annotation.Scope;

@Component
//@Scope("prototype")
public class UserService implements InitializingBean, UserInterface , BeanNameAware {

    @Autowired
    OrderService orderService;

    @JiyuanValue("jiyuan")
    private String name;

    private String beanName;

    @Override
    public void afterPropertiesSet() {
        System.out.println("InitializingBean。。。afterPropertiesSet");
    }

    public void test(){
        System.out.println("test:" + name);
        System.out.println("beanName:"+beanName);
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }
}
