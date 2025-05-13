package com.stratocloud.order.consumer;

import com.stratocloud.event.StratoEventLevel;
import com.stratocloud.jpa.repository.EntityManager;
import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageConsumer;
import com.stratocloud.order.Order;
import com.stratocloud.order.event.OrderEventHandler;
import com.stratocloud.repository.OrderRepository;
import com.stratocloud.utils.JSON;
import com.stratocloud.workflow.messaging.WorkflowReportConfirmStartedPayload;
import com.stratocloud.workflow.messaging.WorkflowTopics;
import com.stratocloud.workflow.runtime.NodeInstance;
import com.stratocloud.workflow.runtime.WorkflowInstance;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class ConfirmStartedConsumer implements MessageConsumer {

    private final EntityManager entityManager;

    private final OrderRepository orderRepository;

    private final OrderEventHandler eventHandler;

    public ConfirmStartedConsumer(EntityManager entityManager,
                                  OrderRepository orderRepository,
                                  OrderEventHandler eventHandler) {
        this.entityManager = entityManager;
        this.orderRepository = orderRepository;
        this.eventHandler = eventHandler;
    }

    @Override
    @Transactional
    public void consume(Message message) {
        var payload = JSON.toJavaObject(message.getPayload(), WorkflowReportConfirmStartedPayload.class);

        NodeInstance nodeInstance = entityManager.findById(NodeInstance.class, payload.nodeInstanceId());
        WorkflowInstance workflowInstance = nodeInstance.getWorkflowInstance();

        Optional<Order> order = orderRepository.findByWorkflowInstanceId(workflowInstance.getId());

        if(order.isEmpty())
            return;

        order.get().onConfirmStarted(payload.nodeInstanceId(), payload.possibleHandlers());

        Order saved = orderRepository.save(order.get());

        eventHandler.handleEvent(
                eventHandler.getEvent(
                        saved,
                        OrderEventHandler.ORDER_APPROVAL_STARTED_EVENT_TYPE,
                        StratoEventLevel.REMIND
                )
        );
    }

    @Override
    public String getTopic() {
        return WorkflowTopics.WORKFLOW_REPORT_CONFIRM_STARTED;
    }

    @Override
    public String getConsumerGroup() {
        return "ORDER";
    }
}
