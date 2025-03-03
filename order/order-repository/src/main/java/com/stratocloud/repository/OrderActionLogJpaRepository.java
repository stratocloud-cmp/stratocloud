package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.AuditableJpaRepository;
import com.stratocloud.order.OrderActionLog;

public interface OrderActionLogJpaRepository extends AuditableJpaRepository<OrderActionLog> {
}
