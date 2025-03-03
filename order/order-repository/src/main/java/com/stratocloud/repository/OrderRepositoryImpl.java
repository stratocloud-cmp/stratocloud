package com.stratocloud.repository;

import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.jpa.repository.AbstractControllableRepository;
import com.stratocloud.order.*;
import com.stratocloud.utils.Utils;
import com.stratocloud.workflow.Workflow;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepositoryImpl extends AbstractControllableRepository<Order, OrderJpaRepository>
        implements OrderRepository {

    public OrderRepositoryImpl(OrderJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findByWorkflowInstanceId(Long id) {
        return jpaRepository.findByWorkflowInstanceId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Order findOrder(Long orderId) {
        return jpaRepository.findById(orderId).orElseThrow(
                () -> new EntityNotFoundException("Order not found.")
        );
    }

    @Override
    public void validatePermission(Order entity) {

    }


    @Override
    public Page<Order> page(OrderFilters orderFilters, Pageable pageable) {
        Specification<Order> spec = getOrderSpecification(orderFilters);
        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<Order> getOrderSpecification(OrderFilters orderFilters) {
        Specification<Order> spec = getCallingTenantSpec();

        if(Utils.isNotEmpty(orderFilters.orderIds()))
            spec = spec.and(getIdSpec(orderFilters.orderIds()));

        if(Utils.isNotEmpty(orderFilters.tenantIds()))
            spec = spec.and(getTenantSpec(orderFilters.tenantIds()));

        if(Utils.isNotEmpty(orderFilters.ownerIds()))
            spec = spec.and(getOwnerSpec(orderFilters.ownerIds()));

        if(Utils.isNotEmpty(orderFilters.possibleHandlerIds()))
            spec = spec.and(getPossiblerHandlerSpec(orderFilters.possibleHandlerIds()));

        if(Utils.isNotEmpty(orderFilters.historyHandlerIds()))
            spec = spec.and(getHistoryHandlerSpec(orderFilters.historyHandlerIds()));

        if(Utils.isEmpty(orderFilters.possibleHandlerIds()) &&
                Utils.isEmpty(orderFilters.historyHandlerIds()) &&
                Utils.isEmpty(orderFilters.orderIds()))
            spec = spec.and(getCallingOwnerSpec());

        if(Utils.isNotEmpty(orderFilters.orderStatuses()))
            spec = spec.and(getStatusSpec(orderFilters.orderStatuses()));

        if(Utils.isNotBlank(orderFilters.search()))
            spec = spec.and(getSearchSpec(orderFilters.search()));

        if(Utils.isNotEmpty(orderFilters.workflowIds()))
            spec = spec.and(getWorkflowSpec(orderFilters.workflowIds()));

        return spec;
    }

    private Specification<Order> getHistoryHandlerSpec(List<Long> historyHandlerIds) {
        return (root, query, criteriaBuilder) -> {
            Join<OrderActionLog, Order> join = root.join("logs");
            return join.get("handlerId").in(historyHandlerIds);
        };
    }

    private Specification<Order> getWorkflowSpec(List<Long> workflowIds) {
        return (root, query, criteriaBuilder) -> {
            Join<Workflow, Order> join = root.join("workflow");
            return join.get("id").in(workflowIds);
        };
    }

    private Specification<Order> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> {
            Predicate p1 = criteriaBuilder.like(root.get("orderNo"), "%" + search + "%");
            Predicate p2 = criteriaBuilder.like(root.get("orderName"), "%" + search + "%");
            Predicate p3 = criteriaBuilder.like(root.get("summary"), "%" + search + "%");
            return criteriaBuilder.or(p1, p2, p3);
        };
    }

    private Specification<Order> getStatusSpec(List<OrderStatus> orderStatuses) {
        return (root, query, criteriaBuilder) -> root.get("status").in(orderStatuses);
    }

    private Specification<Order> getPossiblerHandlerSpec(List<Long> possibleHandlerIds) {
        return (root, query, criteriaBuilder) -> {
            Join<PossibleHandler, Order> join = root.join("possibleHandlers");
            return join.get("userId").in(possibleHandlerIds);
        };
    }
}
