package com.stratocloud.software;

import com.stratocloud.auth.CallContext;
import com.stratocloud.script.SoftwareDefinitionService;
import com.stratocloud.script.cmd.CreateSoftwareDefinitionCmd;
import com.stratocloud.script.cmd.NestedSoftwareRequirement;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
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
import java.util.List;

@Slf4j
@Component
public class BuiltInRedisInitializer implements InitializingBean, ApplicationContextAware {

    private final SoftwareDefinitionService service;

    private ApplicationContext applicationContext;

    public BuiltInRedisInitializer(SoftwareDefinitionService service) {
        this.service = service;
    }


    private String loadJsonContent(String fileName) throws IOException {
        Resource resource = applicationContext.getResource("classpath:software/%s".formatted(fileName));
        return IOUtils.toString(resource.getURI(), Charset.defaultCharset());
    }

    private CreateSoftwareDefinitionCmd getRedisDefinition() throws IOException {
        return JSON.toJavaObject(loadJsonContent("redis.json"), CreateSoftwareDefinitionCmd.class);
    }

    private CreateSoftwareDefinitionCmd getRedisClusterDefinition() throws IOException {
        return JSON.toJavaObject(loadJsonContent("redis_cluster.json"), CreateSoftwareDefinitionCmd.class);
    }



    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        try {
            CallContext.registerSystemSession();

            Long redisDefinitionId = service.createSoftwareDefinition(getRedisDefinition()).getSoftwareDefinitionId();

            CreateSoftwareDefinitionCmd redisClusterDefinition = getRedisClusterDefinition();

            List<NestedSoftwareRequirement> redisClusterRequirements = redisClusterDefinition.getRequirements();

            if(Utils.isNotEmpty(redisClusterRequirements)){
                redisClusterRequirements.stream().filter(
                        r -> "NODE".equals(r.getRequirementKey())
                ).forEach(
                        r -> r.setTargetSoftwareDefinitionId(redisDefinitionId)
                );
            }

            service.createSoftwareDefinition(redisClusterDefinition);

            log.info("Redis and RedisCluster definitions initialized");
        }catch (Exception e){
            log.warn(e.getMessage());
        }finally {
            CallContext.unregister();
        }
    }
}
