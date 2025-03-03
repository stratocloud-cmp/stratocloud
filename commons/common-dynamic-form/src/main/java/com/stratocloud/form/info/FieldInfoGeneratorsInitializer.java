package com.stratocloud.form.info;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class FieldInfoGeneratorsInitializer implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if(!(bean instanceof FieldInfoGenerator generator))
            return bean;
        FieldInfoGeneratorRegistry.register(generator);
        return bean;
    }
}
