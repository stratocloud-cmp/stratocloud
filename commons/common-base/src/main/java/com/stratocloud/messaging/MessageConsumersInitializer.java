package com.stratocloud.messaging;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class MessageConsumersInitializer implements BeanPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if(!(bean instanceof MessageConsumer messageConsumer))
            return bean;

        MessageBus messageBus = applicationContext.getBean(MessageBus.class);
        messageBus.subscribe(messageConsumer);
        return bean;
    }


}
