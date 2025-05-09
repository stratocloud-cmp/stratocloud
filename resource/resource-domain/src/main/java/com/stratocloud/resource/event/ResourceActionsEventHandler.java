package com.stratocloud.resource.event;

import com.stratocloud.event.*;
import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageBus;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.repository.ResourceEventRepository;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceCategory;
import com.stratocloud.utils.Utils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
public class ResourceActionsEventHandler implements EventHandler<ResourceActionEventProperties> {

    private final MessageBus messageBus;

    private final ResourceEventRepository eventRepository;

    public ResourceActionsEventHandler(MessageBus messageBus,
                                       ResourceEventRepository eventRepository) {
        this.messageBus = messageBus;
        this.eventRepository = eventRepository;
    }

    @Override
    @Transactional
    public void handleEvent(StratoEvent<ResourceActionEventProperties> event) {
        ResourceEvent resourceEvent = ResourceEvent.from(event);
        eventRepository.save(resourceEvent);

        messageBus.publishWithSystemSession(
                Message.create(
                        EventTopics.EVENT_NOTIFICATION_TOPIC,
                        EventNotificationPayload.from(event)
                )
        );
    }

    public StratoEventType getEventType(ResourceActionHandler actionHandler, boolean success){
        String eventTypePrefix = "%s.%s".formatted(
                actionHandler.getResourceHandler().getResourceCategory().id(),
                actionHandler.getAction().id()
        );
        String eventTypeSuffix = success ? ".SUCCESS" : ".FAILED";
        String eventType = eventTypePrefix + eventTypeSuffix;

        String eventTypeNamePrefix = "%s%s".formatted(
                actionHandler.getResourceHandler().getResourceCategory().name(),
                actionHandler.getAction().name()
        );
        String eventTypeNameSuffix = success ? "成功" : "失败";
        String eventTypeName = eventTypeNamePrefix + eventTypeNameSuffix;

        return new StratoEventType(
                eventType,
                eventTypeName
        );
    }

    @Override
    public Set<StratoEventType> getSupportedEventTypes(ApplicationContext applicationContext) {
        Map<String, ResourceActionHandler> handlerMap
                = applicationContext.getBeansOfType(ResourceActionHandler.class);

        Set<StratoEventType> eventTypes = new HashSet<>();
        if(Utils.isEmpty(handlerMap))
            return eventTypes;

        for (ResourceActionHandler actionHandler : handlerMap.values()) {
            StratoEventType successEventType = getEventType(actionHandler, true);
            StratoEventType failedEventType = getEventType(actionHandler, false);

            eventTypes.add(successEventType);
            eventTypes.add(failedEventType);
        }

        return eventTypes;
    }

    @Override
    public ResourceActionEventProperties getExampleEventProperties() {
        return ResourceActionEventProperties.createExample();
    }

    @Override
    public List<BuiltInNotificationPolicy> getBuiltInNotificationPolicies() {
        return List.of();
    }

    public StratoEventObject getEventObject(Resource resource) {
        ResourceCategory resourceCategory = resource.getResourceHandler().getResourceCategory();
        return new StratoEventObject(
                resourceCategory.id(),
                resourceCategory.name(),
                resource.getId(),
                resource.getName(),
                resource.getOwnerId()
        );
    }
}
