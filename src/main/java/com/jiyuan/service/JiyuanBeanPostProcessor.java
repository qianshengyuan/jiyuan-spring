package com.jiyuan.service;

import com.spring.BeanPostProcessor;
import com.spring.annotation.Component;
import com.spring.annotation.JiyuanValue;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

@Component
public class JiyuanBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println(beanName + ":postProcessBeforeInitialization");
        for (Field field : bean.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(JiyuanValue.class)) {
                field.setAccessible(true);
                String value = field.getAnnotation(JiyuanValue.class).value();
                try {
                    field.set(bean, value);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 做一个aop，狸猫换太子，对原始的bean对象进行操作
        Object instance = Proxy.newProxyInstance(this.getClass().getClassLoader(), bean.getClass().getInterfaces(), (proxy, method, args) -> {
            // 切面逻辑
            System.out.println("invoke调用");
            // 调用原始对象的方法
            return method.invoke(bean, args);
        });


        return instance;
    }
}
