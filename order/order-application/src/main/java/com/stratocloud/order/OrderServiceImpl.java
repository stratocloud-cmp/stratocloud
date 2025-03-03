package com.stratocloud.order;

import com.stratocloud.audit.AuditLogContext;
import com.stratocloud.audit.AuditObject;
import com.stratocloud.auth.CallContext;
import com.stratocloud.auth.UserSession;
import com.stratocloud.external.order.JobHandlerGatewayService;
import com.stratocloud.order.cmd.*;
import com.stratocloud.order.query.DescribeOrdersRequest;
import com.stratocloud.order.query.DescribeRollbackTargetsRequest;
import com.stratocloud.order.query.DescribeRollbackTargetsResponse;
import com.stratocloud.order.query.NestedOrderResponse;
import com.stratocloud.order.response.*;
import com.stratocloud.repository.OrderRepository;
import com.stratocloud.repository.WorkflowRepository;
import com.stratocloud.utils.Utils;
import com.stratocloud.validate.ValidateRequest;
import com.stratocloud.workflow.Workflow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderFactory orderFactory;
    private final OrderRepository repository;

    private final OrderAssembler assembler;

    private final WorkflowRepository workflowRepository;


    private final JobHandlerGatewayService jobHandlerGatewayService;

    public OrderServiceImpl(OrderFactory orderFactory,
                            OrderRepository repository,
                            OrderAssembler assembler,
                            WorkflowRepository workflowRepository,
                            JobHandlerGatewayService jobHandlerGatewayService) {
        this.orderFactory = orderFactory;
        this.repository = repository;
        this.assembler = assembler;
        this.workflowRepository = workflowRepository;
        this.jobHandlerGatewayService = jobHandlerGatewayService;
    }

    @Override
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderCmd cmd) {
        Workflow workflow = workflowRepository.findWorkflow(cmd.getWorkflowId());

        Workflow replica = workflow.createReplica();

        replica = workflowRepository.saveWithSystemSession(replica);

        Order order = orderFactory.createOrder(cmd, replica);

        order = repository.save(order);

        AuditLogContext.current().addAuditObject(
                new AuditObject(order.getId().toString(), order.getOrderNo())
        );

        return new CreateOrderResponse(order.getId());
    }

    @Override
    @Transactional
    public UpdateOrderItemResponse updateOrderItem(UpdateOrderItemCmd cmd) {
        Long orderId = cmd.getOrderId();
        Long orderItemId = cmd.getOrderItemId();
        Map<String, Object> parameters = cmd.getParameters();

        Order order = repository.findOrder(orderId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(order.getId().toString(), order.getOrderNo())
        );

        order.onUpdateItem(orderItemId, parameters);

        repository.save(order);

        return new UpdateOrderItemResponse();
    }

    @Override
    @Transactional
    public SubmitOrderResponse submitOrder(SubmitOrderCmd cmd) {
        Long orderId = cmd.getOrderId();
        String message = cmd.getMessage();
        Order order = repository.findOrder(orderId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(order.getId().toString(), order.getOrderNo())
        );

        order.onSubmit(message, Map.of());

        for (OrderItem orderItem : order.getOrderItems())
            jobHandlerGatewayService.preCreateJob(orderItem.getJobNodeInstance().getJob());

        order = repository.save(order);

        order.collectSummaryData();
        repository.save(order);

        return new SubmitOrderResponse();
    }

    @Override
    @Transactional
    public ApproveOrderResponse approveOrder(ApproveOrderCmd cmd) {
        Order order = repository.findOrder(cmd.getOrderId());

        AuditLogContext.current().addAuditObject(
                new AuditObject(order.getId().toString(), order.getOrderNo())
        );

        UserSession callingUser = CallContext.current().getCallingUser();
        order.onApprove(callingUser.userId(), cmd.getMessage());
        repository.save(order);
        return new ApproveOrderResponse();
    }

    @Override
    @Transactional
    public ConfirmOrderResponse confirmOrder(ConfirmOrderCmd cmd) {
        Order order = repository.findOrder(cmd.getOrderId());

        AuditLogContext.current().addAuditObject(
                new AuditObject(order.getId().toString(), order.getOrderNo())
        );

        UserSession callingUser = CallContext.current().getCallingUser();
        order.onConfirm(callingUser.userId(), cmd.getMessage());
        repository.save(order);
        return new ConfirmOrderResponse();
    }

    @Override
    @Transactional
    public RollbackOrderResponse rollbackOrder(RollbackOrderCmd cmd) {
        Order order = repository.findOrder(cmd.getOrderId());

        AuditLogContext.current().addAuditObject(
                new AuditObject(order.getId().toString(), order.getOrderNo())
        );

        order.onRollback(cmd.getNodeId(), cmd.getMessage());
        repository.save(order);
        return new RollbackOrderResponse();
    }


    @Override
    @Transactional
    public RejectOrderResponse rejectOrder(RejectOrderCmd cmd) {
        Order order = repository.findOrder(cmd.getOrderId());

        AuditLogContext.current().addAuditObject(
                new AuditObject(order.getId().toString(), order.getOrderNo())
        );

        order.onReject(cmd.getMessage());
        repository.save(order);
        return new RejectOrderResponse();
    }


    @Override
    @Transactional
    public CancelOrderResponse cancelOrder(CancelOrderCmd cmd) {
        Order order = repository.findOrder(cmd.getOrderId());

        AuditLogContext.current().addAuditObject(
                new AuditObject(order.getId().toString(), order.getOrderNo())
        );

        order.onCancel(cmd.getMessage());
        repository.save(order);
        return new CancelOrderResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public DenyOrderResponse denyOrder(DenyOrderCmd cmd) {
        Order order = repository.findOrder(cmd.getOrderId());

        AuditLogContext.current().addAuditObject(
                new AuditObject(order.getId().toString(), order.getOrderNo())
        );

        order.onDeny(cmd.getMessage());
        repository.save(order);
        return new DenyOrderResponse();
    }


    @Override
    @Transactional
    @ValidateRequest
    public CloneOrderResponse cloneOrder(CloneOrderCmd cmd) {
        Order order = repository.findOrder(cmd.getOrderId());

        AuditLogContext.current().addAuditObject(
                new AuditObject(order.getId().toString(), order.getOrderNo())
        );

        CreateOrderCmd createOrderCmd = new CreateOrderCmd();

        createOrderCmd.setNote(order.getNote());
        createOrderCmd.setWorkflowId(order.getWorkflow().getId());

        List<NestedOrderItemCmd> nestedOrderItems = new ArrayList<>();

        if(Utils.isNotEmpty(order.getOrderItems())){
            for (OrderItem orderItem : order.getOrderItems()) {
                NestedOrderItemCmd nestedOrderItemCmd = new NestedOrderItemCmd();
                nestedOrderItemCmd.setJobNodeKey(orderItem.getJobNode().getNodeKey());
                nestedOrderItemCmd.setParameters(orderItem.getParameters());
                nestedOrderItems.add(nestedOrderItemCmd);
            }
        }
        createOrderCmd.setItems(nestedOrderItems);

        CreateOrderResponse orderResponse = createOrder(createOrderCmd);

        return new CloneOrderResponse(orderResponse.getOrderId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NestedOrderResponse> describeOrders(DescribeOrdersRequest request) {
        List<Long> orderIds = request.getOrderIds();
        List<Long> tenantIds = request.getTenantIds();
        List<Long> ownerIds = request.getOwnerIds();
        List<Long> possibleHandlerIds = request.getPossibleHandlerIds();
        List<Long> historyHandlerIds = request.getHistoryHandlerIds();
        List<OrderStatus> orderStatuses = request.getOrderStatuses();
        String search = request.getSearch();
        List<Long> workflowIds = request.getWorkflowIds();
        Pageable pageable = request.getPageable();

        OrderFilters orderFilters = new OrderFilters(
                orderIds, tenantIds, ownerIds, possibleHandlerIds, historyHandlerIds, orderStatuses, search, workflowIds
        );

        Page<Order> page = repository.page(orderFilters, pageable);
        return assembler.convertPage(page);
    }

    @Override
    @Transactional(readOnly = true)
    public DescribeRollbackTargetsResponse describeRollbackTargets(DescribeRollbackTargetsRequest request) {
        Order order = repository.findOrder(request.getOrderId());
        return assembler.toDescribeRollbackTargetsResponse(order);
    }
}
