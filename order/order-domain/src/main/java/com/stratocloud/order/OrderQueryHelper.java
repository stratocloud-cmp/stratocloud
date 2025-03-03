package com.stratocloud.order;

import com.stratocloud.jpa.repository.EntityManager;
import com.stratocloud.repository.OrderRepository;
import com.stratocloud.workflow.runtime.NodeInstance;
import com.stratocloud.workflow.runtime.WorkflowInstance;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OrderQueryHelper {

    private final EntityManager entityManager;

    private final OrderRepository orderRepository;

    public OrderQueryHelper(EntityManager entityManager, OrderRepository orderRepository) {
        this.entityManager = entityManager;
        this.orderRepository = orderRepository;
    }

    public Optional<Order> findByNodeInstanceId(Long nodeInstanceId){
        NodeInstance nodeInstance = entityManager.findById(NodeInstance.class, nodeInstanceId);
        WorkflowInstance workflowInstance = nodeInstance.getWorkflowInstance();

        return orderRepository.findByWorkflowInstanceId(workflowInstance.getId());
    }


}
