package com.stratocloud.resource.event;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.event.*;
import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageBus;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.repository.ResourceEventRepository;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceCategory;
import com.stratocloud.utils.Utils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
public class ResourceRelationshipsEventHandler implements EventHandler<ResourceRelationshipEventProperties> {

    private final MessageBus messageBus;

    private final ResourceEventRepository eventRepository;

    public ResourceRelationshipsEventHandler(MessageBus messageBus,
                                             ResourceEventRepository eventRepository) {
        this.messageBus = messageBus;
        this.eventRepository = eventRepository;
    }

    @Override
    public void handleEvent(StratoEvent<ResourceRelationshipEventProperties> event) {
        ResourceEvent resourceEvent = ResourceEvent.from(event);
        eventRepository.save(resourceEvent);

        messageBus.publishWithSystemSession(
                Message.create(
                        EventTopics.EVENT_NOTIFICATION_TOPIC,
                        EventNotificationPayload.from(event)
                )
        );
    }

    @Override
    public Set<StratoEventType> getSupportedEventTypes(ApplicationContext applicationContext) {
        Map<String, RelationshipHandler> handlerMap
                = applicationContext.getBeansOfType(RelationshipHandler.class);

        Set<StratoEventType> eventTypes = new HashSet<>();
        if(Utils.isEmpty(handlerMap))
            return eventTypes;

        for (RelationshipHandler relationshipHandler : handlerMap.values()) {
            if(relationshipHandler.supportConnectEvent()){
                StratoEventType successConnectEventType = getEventType(
                        relationshipHandler, true, true
                );
                StratoEventType failedConnectEventType = getEventType(
                        relationshipHandler, true, false
                );
                eventTypes.add(successConnectEventType);
                eventTypes.add(failedConnectEventType);
            }
            if(relationshipHandler.supportDisconnectEvent()){
                StratoEventType successDisconnectEventType = getEventType(
                        relationshipHandler, false, true
                );
                StratoEventType failedDisconnectEventType = getEventType(
                        relationshipHandler, false, false
                );


                eventTypes.add(successDisconnectEventType);
                eventTypes.add(failedDisconnectEventType);
            }
        }

        return eventTypes;
    }

    public StratoEventType getEventType(RelationshipHandler relationshipHandler,
                                        boolean connectAction,
                                        boolean success){
        String eventTypePrefix = "%s.%s.%s".formatted(
                relationshipHandler.getSource().getResourceCategory().id(),
                connectAction ? "CONNECT_TO" : "DISCONNECT_FROM",
                relationshipHandler.getTarget().getResourceCategory().id()
        );
        String eventTypeSuffix = success ? ".SUCCESS" : ".FAILED";
        String eventType = eventTypePrefix + eventTypeSuffix;

        String eventTypeNamePrefix = "%s%s".formatted(
                relationshipHandler.getSource().getResourceCategory().name(),
                connectAction ? relationshipHandler.getConnectActionName() : relationshipHandler.getDisconnectActionName()
        );
        String eventTypeNameSuffix = success ? "成功" : "失败";
        String eventTypeName = eventTypeNamePrefix + eventTypeNameSuffix;

        return new StratoEventType(
                eventType,
                eventTypeName
        );
    }

    @Override
    public ResourceRelationshipEventProperties getExampleEventProperties() {
        return ResourceRelationshipEventProperties.createExample();
    }

    @Override
    public List<BuiltInNotificationPolicy> getBuiltInNotificationPolicies() {
        return List.of();
    }


    public StratoEventObject getEventObject(Relationship relationship) {
        Resource source = relationship.getSource();
        ResourceCategory resourceCategory = source.getResourceHandler().getResourceCategory();
        return new StratoEventObject(
                resourceCategory.id(),
                resourceCategory.name(),
                source.getId(),
                source.getName(),
                source.getOwnerId()
        );
    }


    public StratoEvent<ResourceRelationshipEventProperties> getEvent(Relationship relationship,
                                                                     boolean connectAction,
                                                                     boolean success) {
        Resource source = relationship.getSource();
        ExternalAccount account
                = source.getResourceHandler().getAccountRepository().findExternalAccount(source.getAccountId());

        ResourceRelationshipEventProperties eventProperties
                = ResourceRelationshipEventProperties.create(relationship, account, true);

        StratoEventType eventType = getEventType(
                relationship.getHandler(),
                connectAction,
                success
        );
        return new StratoEvent<>(
                UUID.randomUUID().toString(),
                eventType,
                success ? StratoEventLevel.INFO : StratoEventLevel.WARNING,
                StratoEventSource.STRATO_ACTION,
                getEventObject(relationship),
                eventType+": "+source.getName(),
                LocalDateTime.now(),
                eventProperties
        );
    }
}
