package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.ControllableJpaRepository;
import com.stratocloud.order.Order;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface OrderJpaRepository extends ControllableJpaRepository<Order>, JpaSpecificationExecutor<Order> {
    Optional<Order> findByWorkflowInstanceId(Long id);
}
