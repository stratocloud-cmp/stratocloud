package com.stratocloud.secrets;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class SecretsManagerInitializer implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if(bean instanceof SecretsManager secretsManager)
            SecretsManagerRegistry.register(secretsManager);

        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }
}
