package com.stratocloud.provider.script;

import com.stratocloud.provider.Provider;
import com.stratocloud.provider.dynamic.DynamicResourceHandlerLoader;
import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.script.software.SoftwareHandlerLoader;
import com.stratocloud.provider.script.init.InitScriptHandlerLoader;
import com.stratocloud.provider.script.standalone.GuestOsExecuteScriptHandler;
import com.stratocloud.utils.ContextUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SoftwareAndScriptInitializer implements ApplicationContextAware, BeanPostProcessor {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if(!(bean instanceof GuestOsHandler guestOsHandler))
            return bean;

        Provider provider = guestOsHandler.getProvider();
        List<DynamicResourceHandlerLoader> existedLoaders = provider.getResourceHandlerLoaders();

        boolean alreadyRegistered = existedLoaders.stream().anyMatch(
                loader -> loader instanceof SoftwareHandlerLoader
        );

        if(alreadyRegistered)
            return bean;

        SoftwareHandlerLoader softwareHandlerLoader = new SoftwareHandlerLoader(provider);
        softwareHandlerLoader = ContextUtil.ensureBean(
                applicationContext,
                softwareHandlerLoader,
                "%s_SOFTWARE_HANDLER_LOADER".formatted(provider.getId())
        );

        provider.registerLoader(softwareHandlerLoader);

        InitScriptHandlerLoader scriptHandlerLoader = new InitScriptHandlerLoader(provider);
        scriptHandlerLoader = ContextUtil.ensureBean(
                applicationContext,
                scriptHandlerLoader,
                "%s_SCRIPT_HANDLER_LOADER".formatted(provider.getId())
        );

        provider.registerLoader(scriptHandlerLoader);

        GuestOsExecuteScriptHandler executeScriptHandler = new GuestOsExecuteScriptHandler(guestOsHandler);
        executeScriptHandler = ContextUtil.ensureBean(
                applicationContext,
                executeScriptHandler,
                "%s_EXECUTE_SCRIPT_HANDLER".formatted(guestOsHandler.getResourceTypeId())
        );
        guestOsHandler.registerActionHandler(executeScriptHandler);

        return bean;
    }
}
