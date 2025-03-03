package com.stratocloud.job;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class TaskHandlersInitializer implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if(!(bean instanceof TaskHandler taskHandler))
            return bean;

        TaskHandlerRegistry.register(taskHandler);
        return bean;
    }
}
