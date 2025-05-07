package com.stratocloud.notification;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;


@Component
public class NotificationProvidersInitializer implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if(bean instanceof NotificationProvider provider)
            NotificationProviderRegistry.register(provider);

        return bean;
    }
}
