package com.stratocloud.user;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class UserAuthenticatorsInitializer implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if(!(bean instanceof UserAuthenticator authenticator))
            return bean;
        UserAuthenticatorRegistry.register(authenticator);
        return bean;
    }
}
