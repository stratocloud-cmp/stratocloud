package com.stratocloud.repository;

import com.stratocloud.jpa.repository.ControllableRepository;
import com.stratocloud.order.Order;
import com.stratocloud.order.OrderFilters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface OrderRepository extends ControllableRepository<Order> {
    Optional<Order> findByWorkflowInstanceId(Long id);

    Order findOrder(Long orderId);

    Page<Order> page(OrderFilters orderFilters, Pageable pageable);
}
