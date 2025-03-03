package com.stratocloud.utils;

import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ContextUtil implements ApplicationContextAware {
    @Getter
    private static volatile ApplicationContext applicationContext;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ContextUtil.applicationContext = applicationContext;
    }


    public static <T> T getBean(Class<T> clazz){
        return applicationContext.getBean(clazz);
    }


    @SuppressWarnings("unchecked")
    public static <T> T ensureBean(ApplicationContext applicationContext, T bean, String beanName){
        Class<T> beanClass = (Class<T>) bean.getClass();

        if(applicationContext.containsBean(beanName)){
            return bean;
        }

        BeanDefinition beanDefinition = new RootBeanDefinition(beanClass, ()->bean);
        DefaultListableBeanFactory beanFactory
                = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        beanFactory.registerBeanDefinition(beanName, beanDefinition);

        return applicationContext.getBean(beanName, beanClass);
    }
}
