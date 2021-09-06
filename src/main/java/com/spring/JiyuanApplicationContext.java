package com.spring;

import com.spring.annotation.Autowired;
import com.spring.annotation.Component;
import com.spring.annotation.ComponentScan;
import com.spring.annotation.Scope;

import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JiyuanApplicationContext {

    private Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();

    private Map<String, Object> singletonObjects = new HashMap<>();

    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    public JiyuanApplicationContext(Class configClass) {
        // 扫描所有组件
        // 放入beanDefinitionMap中
        scan(configClass);

        // 实例化非懒加载的单例bean
        instanceSingletonBean();
    }

    private void scan(Class configClass){
        if (configClass.isAnnotationPresent(ComponentScan.class)) {
            ComponentScan componentScanAnnotation = (ComponentScan) configClass.getAnnotation(ComponentScan.class);
            String path = componentScanAnnotation.value(); // ComponentScan注解配置的扫描路径
            path = path.replace(".","/");

            // 获取路径下对应的所有class文件
            ClassLoader classLoader = configClass.getClassLoader();
            URL resource = classLoader.getResource(path);
            File file = new File(resource.getFile());
            if(file.isDirectory()){
                for (File f : file.listFiles()) {
                    String absolutePath = f.getAbsolutePath();
                    try {
                        // 加载， 类路径格式为com.jiyuan.service.userService
                        String classPath = absolutePath.substring(absolutePath.indexOf("com"),absolutePath.indexOf(".class")).replace("\\",".");
                        Class<?> clazz = classLoader.loadClass(classPath);
                        if (clazz.isAnnotationPresent(Component.class)) {
                            // 将bean的信息保存下来
                            BeanDefinition beanDefinition = new BeanDefinition();
                            beanDefinition.setType(clazz);

                            // 保存bean的后置处理器
                            if(BeanPostProcessor.class.isAssignableFrom(clazz)){
                                // 如果是bean的后置处理器，需要先实例化出来
                                BeanPostProcessor beanPostProcessor = (BeanPostProcessor) clazz.getConstructor().newInstance();
                                beanPostProcessorList.add(beanPostProcessor);
                            }

                            Component componentAnnotation = clazz.getAnnotation(Component.class);
                            String beanName = componentAnnotation.value();
                            // 默认beanName类名的首字母小写
                            if("".equals(beanName)){
                                beanName = Introspector.decapitalize(clazz.getSimpleName());
                            }

                            // 判断Scope注解
                            if(clazz.isAnnotationPresent(Scope.class)){
                                Scope scopeAnnotation = clazz.getAnnotation(Scope.class);
                                String scope = scopeAnnotation.value();
                                beanDefinition.setScope(scope);
                            } else {
                                // 没写，默认单例
                                beanDefinition.setScope("singleton");
                            }

                            // 判断是否lazy

                            // 将beanDefinition进行缓存
                            beanDefinitionMap.put(beanName, beanDefinition);
                        }


                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    /**
     * 实例化所有非懒加载的单例bean
     */
    private void instanceSingletonBean() {
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            String beanName = entry.getKey();
            BeanDefinition beanDefinition = entry.getValue();
            if("singleton".equals(beanDefinition.getScope())){
                createBean(beanName, beanDefinition);
            }
        }
    }

    /*
     * @Author JiYuan
     * @Date 2021/9/6 13:19
     * @Description 创建bean
     */
    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        Object bean = null;
        try {
            bean = beanDefinition.getType().getConstructor().newInstance();

            // 为bean属性赋值
            for (Field field : bean.getClass().getDeclaredFields()) {
                if(field.isAnnotationPresent(Autowired.class)){
                    field.setAccessible(true);
                    field.set(bean, getBean(field.getName()));
                }
            }

            //Aware
            if(bean instanceof BeanNameAware){
                ((BeanNameAware)bean).setBeanName(beanName);
            }

            // bean后置处理器 初始化前
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                // 这里有可能返回的是代理对象
                beanPostProcessor.postProcessBeforeInitialization(bean, beanName);
            }

            // bean初始化
            if(bean instanceof InitializingBean){
                ((InitializingBean)bean).afterPropertiesSet();
            }

            // bean后置处理器 初始化后
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                // 这里有可能返回的是代理对象
                beanPostProcessor.postProcessAfterInitialization(bean, beanName);
            }

            // 将单例bean存入单例池中
            if("singleton".equals(beanDefinition.getScope())){
                singletonObjects.put(beanName, bean);
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return bean;
    }

    public Object getBean(String beanName) {
        if(!beanDefinitionMap.containsKey(beanName)){
            throw new RuntimeException("无此bean定义！beanName：" + beanName);
        }
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if("singleton".equals(beanDefinition.getScope())){
            Object bean = singletonObjects.get(beanName);
            if(null == bean){
                bean = createBean(beanName, beanDefinition);
            }
            return bean;
        } else {
            // 原型bean 每次都创建
            return createBean(beanName, beanDefinition);
        }
    }
}
