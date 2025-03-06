package com.stratocloud.provider;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.utils.GraphUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ProvidersInitializer implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        initializeProviders(bean);
        initializeResourceHandlers(bean);
        initializeResourceActionHandlers(bean);
        initializeResourceReadActionHandlers(bean);
        initializeRelationshipHandlers(bean);
        return bean;
    }

    private void initializeProviders(Object bean) {
        if(!(bean instanceof Provider provider))
            return;
        ProviderRegistry.register(provider);
        log.info("Provider {} registered.", provider.getName());
    }

    private void initializeResourceHandlers(Object bean) {
        if(!(bean instanceof ResourceHandler resourceHandler))
            return;
        resourceHandler.getProvider().register(resourceHandler);
        log.info("Resource handler {} registered.", resourceHandler.getClass().getSimpleName());
    }

    private void initializeResourceActionHandlers(Object bean) {
        if(!(bean instanceof ResourceActionHandler resourceActionHandler))
            return;
        resourceActionHandler.getResourceHandler().registerActionHandler(resourceActionHandler);
        log.info("Resource action handler {} registered.", resourceActionHandler.getClass().getSimpleName());
    }

    private void initializeResourceReadActionHandlers(Object bean) {
        if(!(bean instanceof ResourceReadActionHandler readActionHandler))
            return;
        readActionHandler.getResourceHandler().registerReadActionHandler(readActionHandler);
        log.info("Resource read action handler {} registered.", readActionHandler.getClass().getSimpleName());
    }

    private void initializeRelationshipHandlers(Object bean) {
        if(!(bean instanceof RelationshipHandler relationshipHandler))
            return;

        ResourceHandler target = relationshipHandler.getTarget();
        ResourceHandler source = relationshipHandler.getSource();

        List<ResourceHandler> requirementsTree = GraphUtil.bfs(target, ResourceHandler::getRequirementsTargets);

        if(requirementsTree.contains(source))
            throw new StratoException(
                    "Circular relationship %s detected in provider %s.".formatted(
                            relationshipHandler.getRelationshipTypeId(),
                            target.getProvider().getId()
                    )
            );

        target.registerCapability(relationshipHandler);
        source.registerRequirement(relationshipHandler);
        log.info("Relationship handler {} registered.", relationshipHandler.getClass().getSimpleName());
    }
}
