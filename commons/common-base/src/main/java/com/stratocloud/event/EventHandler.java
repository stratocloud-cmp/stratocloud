package com.stratocloud.event;

import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageBus;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public interface EventHandler<P extends EventProperties> extends ApplicationContextAware {
    void handleEvent(StratoEvent<P> event);

    default void handleEventQuietly(StratoEvent<P> event){
        try {
            @SuppressWarnings("unchecked")
            EventHandler<P> eventHandler = (EventHandler<P>) AopContext.currentProxy();
            eventHandler.handleEvent(event);
        }catch (Exception e){
            LoggerFactory.getLogger(getClass()).warn("Failed to handle event.", e);
        }
    }

    Set<StratoEventType> getSupportedEventTypes(ApplicationContext applicationContext);

    P getExampleEventProperties();

    List<BuiltInNotificationPolicy> getBuiltInNotificationPolicies();

    @Override
    default void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Set<StratoEventType> eventTypes = getSupportedEventTypes(applicationContext);

        if(Utils.isEmpty(eventTypes))
            return;

        MessageBus messageBus = applicationContext.getBean(MessageBus.class);
        messageBus.publishWithSystemSession(
                Message.create(
                        EventTopics.EVENT_TYPES_INITIALIZE_TOPIC,
                        new InitEventTypesPayload(
                                new ArrayList<>(eventTypes),
                                JSON.toMap(getExampleEventProperties()),
                                getBuiltInNotificationPolicies()
                        )
                )
        );
    }
}
