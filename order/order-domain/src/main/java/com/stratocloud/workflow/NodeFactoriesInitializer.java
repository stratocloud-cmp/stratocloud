package com.stratocloud.workflow;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class NodeFactoriesInitializer implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if(!(bean instanceof NodeFactory<?> nodeFactory))
            return bean;
        NodeFactoryRegistry.register(nodeFactory);
        return bean;
    }
}
