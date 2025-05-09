package com.stratocloud.order.event;

import com.stratocloud.event.*;
import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageBus;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class OrderEventHandler implements EventHandler<OrderEventProperties> {

    private final MessageBus messageBus;

    public static final StratoEventType ORDER_FINISHED_EVENT_TYPE = new StratoEventType(
            "ORDER.FINISHED", "工单执行成功"
    );

    public static final StratoEventType ORDER_FAILED_EVENT_TYPE = new StratoEventType(
            "ORDER.FAILED", "工单执行失败"
    );

    public static final StratoEventType ORDER_APPROVAL_STARTED_EVENT_TYPE = new StratoEventType(
            "ORDER.APPROVAL.STARTED", "工单审批"
    );


    public OrderEventHandler(MessageBus messageBus) {
        this.messageBus = messageBus;
    }

    @Override
    public void handleEvent(StratoEvent<OrderEventProperties> event) {
        messageBus.publishWithSystemSession(
                Message.create(
                        EventTopics.EVENT_NOTIFICATION_TOPIC,
                        EventNotificationPayload.from(event)
                )
        );
    }

    @Override
    public Set<StratoEventType> getSupportedEventTypes(ApplicationContext applicationContext) {
        return Set.of(
                ORDER_FINISHED_EVENT_TYPE,
                ORDER_FAILED_EVENT_TYPE,
                ORDER_APPROVAL_STARTED_EVENT_TYPE
        );
    }

    @Override
    public OrderEventProperties getExampleEventProperties() {
        return OrderEventProperties.createExample();
    }

    @Override
    public List<BuiltInNotificationPolicy> getBuiltInNotificationPolicies() {
        return List.of();
    }
}
