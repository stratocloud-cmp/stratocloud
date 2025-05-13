package com.stratocloud.order.event;

import com.stratocloud.event.*;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageBus;
import com.stratocloud.order.Order;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
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
        try {
            messageBus.publishWithSystemSession(
                    Message.create(
                            EventTopics.EVENT_NOTIFICATION_TOPIC,
                            EventNotificationPayload.from(event)
                    )
            );
        }catch (Exception e){
            log.info("Failed to handle order event.", e);
        }
    }

    public StratoEvent<OrderEventProperties> getEvent(Order order,
                                                      StratoEventType eventType,
                                                      StratoEventLevel eventLevel) {
        OrderEventProperties eventProperties = OrderEventProperties.create(order);
        return new StratoEvent<>(
                UUID.randomUUID().toString(),
                eventType,
                eventLevel,
                StratoEventSource.ORDER,
                new StratoEventObject(
                        "Order",
                        "工单",
                        order.getId(),
                        order.getOrderNo(),
                        order.getOwnerId(),
                        eventProperties.getPossibleHandlerIds()
                ),
                eventType.name() + ": " + order.getOrderNo(),
                LocalDateTime.now(),
                eventProperties
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
    public List<BuiltInNotificationPolicy> getBuiltInNotificationPolicies(ApplicationContext applicationContext) {
        BuiltInNotificationPolicy orderApprovalStartedPolicy = new BuiltInNotificationPolicy(
                ORDER_APPROVAL_STARTED_EVENT_TYPE,
                "ORDER_APPROVAL_STARTED_NOTIFICATION",
                "工单审批通知",
                null,
                "ORDER_HANDLERS",
                null,
                null,
                null,
                new BuiltInNotificationWay(
                        "INTERNAL_MAIL","站内信",null, null
                ),
                loadTemplate(applicationContext, "classpath:templates/OrderApprovalStartedNotification.html"),
                1,
                30
        );
        BuiltInNotificationPolicy orderFinishedPolicy = new BuiltInNotificationPolicy(
                ORDER_FINISHED_EVENT_TYPE,
                "ORDER_FINISHED_NOTIFICATION",
                "工单执行成功通知",
                null,
                "EVENT_OBJECT_OWNER",
                null,
                null,
                null,
                new BuiltInNotificationWay(
                        "INTERNAL_MAIL","站内信",null, null
                ),
                loadTemplate(applicationContext, "classpath:templates/OrderFinishedNotification.html"),
                1,
                30
        );
        BuiltInNotificationPolicy orderFailedPolicy = new BuiltInNotificationPolicy(
                ORDER_FAILED_EVENT_TYPE,
                "ORDER_FAILED_NOTIFICATION",
                "工单执行失败通知",
                null,
                "EVENT_OBJECT_OWNER",
                null,
                null,
                null,
                new BuiltInNotificationWay(
                        "INTERNAL_MAIL","站内信",null, null
                ),
                loadTemplate(applicationContext, "classpath:templates/OrderFailedNotification.html"),
                1,
                30
        );
        return List.of(
                orderApprovalStartedPolicy,
                orderFinishedPolicy,
                orderFailedPolicy
        );
    }

    private String loadTemplate(ApplicationContext applicationContext, String path){
        Resource resource = applicationContext.getResource(path);

        try {
            return IOUtils.toString(resource.getURI(), Charset.defaultCharset());
        } catch (IOException e) {
            throw new StratoException(e.getMessage(), e);
        }
    }
}
