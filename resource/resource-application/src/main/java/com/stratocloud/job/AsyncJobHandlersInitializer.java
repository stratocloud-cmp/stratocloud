package com.stratocloud.job;

import com.stratocloud.utils.ContextUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class AsyncJobHandlersInitializer implements ApplicationContextAware, BeanPostProcessor {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if(!(bean instanceof AsyncJobHandler<?> asyncJobHandler))
            return bean;

        if(asyncJobHandler instanceof AsyncJobHandlerAdaptor<?>)
            return bean;

        AsyncJobHandlerAdaptor<?> adaptor = new AsyncJobHandlerAdaptor<>(asyncJobHandler);

        AsyncJobHandler<?> adaptorBean = ContextUtil.ensureBean(
                applicationContext, adaptor, beanName + "Adaptor"
        );

        JobHandlerRegistry.register(adaptorBean, applicationContext);

        return bean;
    }


}
