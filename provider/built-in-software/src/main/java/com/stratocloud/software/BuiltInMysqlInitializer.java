package com.stratocloud.software;

import com.stratocloud.auth.CallContext;
import com.stratocloud.script.SoftwareDefinitionService;
import com.stratocloud.script.cmd.CreateSoftwareDefinitionCmd;
import com.stratocloud.utils.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;

@Slf4j
@Component
public class BuiltInMysqlInitializer implements InitializingBean, ApplicationContextAware {

    private final SoftwareDefinitionService service;

    private ApplicationContext applicationContext;

    public BuiltInMysqlInitializer(SoftwareDefinitionService service) {
        this.service = service;
    }


    private String loadJsonContent() throws IOException {
        Resource resource = applicationContext.getResource("classpath:software/mysql.json");
        return IOUtils.toString(resource.getURI(), Charset.defaultCharset());
    }

    private CreateSoftwareDefinitionCmd getMysqlDefinition() throws IOException {
        return JSON.toJavaObject(loadJsonContent(), CreateSoftwareDefinitionCmd.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        try {
            CallContext.registerSystemSession();

            service.createSoftwareDefinition(getMysqlDefinition());

            log.info("Mysql definition initialized");
        }catch (Exception e){
            log.warn(e.getMessage());
        }finally {
            CallContext.unregister();
        }
    }
}
