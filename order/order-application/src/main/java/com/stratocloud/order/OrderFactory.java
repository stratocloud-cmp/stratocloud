package com.stratocloud.order;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.order.cmd.NestedOrderItemCmd;

import com.stratocloud.auth.CallContext;
import com.stratocloud.auth.UserSession;
import com.stratocloud.external.order.RuleGatewayService;
import com.stratocloud.job.JobDefinition;
import com.stratocloud.order.cmd.CreateOrderCmd;
import com.stratocloud.order.rules.OrderRuleTypes;
import com.stratocloud.utils.Utils;
import com.stratocloud.workflow.Workflow;
import com.stratocloud.workflow.nodes.JobNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OrderFactory {

    private final RuleGatewayService ruleGatewayService;

    public OrderFactory(RuleGatewayService ruleGatewayService) {
        this.ruleGatewayService = ruleGatewayService;
    }

    public Order createOrder(String note,
                             Workflow workflow,
                             String jobNodeKey,
                             Map<String, Object> jobParameters) {

        CreateOrderCmd createOrderCmd = new CreateOrderCmd();
        createOrderCmd.setNote(note);
        createOrderCmd.setWorkflowId(workflow.getId());

        NestedOrderItemCmd orderItemCmd = new NestedOrderItemCmd();
        orderItemCmd.setJobNodeKey(jobNodeKey);
        orderItemCmd.setParameters(jobParameters);

        createOrderCmd.setItems(List.of(orderItemCmd));

        return createOrder(createOrderCmd, workflow);
    }

    private static Map<String, Object> createRuleArgs(List<Map<String, Object>> jobParameters,
                                                      List<JobDefinition> jobDefinitions) {
        UserSession callingUser = CallContext.current().getCallingUser();
        Map<String, Object> args = new HashMap<>();
        args.put("ownerId", callingUser.userId());
        args.put("tenantId", callingUser.tenantId());
        args.put("jobDefinitions", jobDefinitions);
        args.put("jobParameters", jobParameters);
        return args;
    }

    public Order createOrder(CreateOrderCmd cmd, Workflow workflow) {
        String orderNo = cmd.getOrderNo();
        String orderName = cmd.getOrderName();
        String note = cmd.getNote();
        List<NestedOrderItemCmd> items = cmd.getItems();



        List<JobNode> jobNodes = workflow.getNodesByType(JobNode.class);
        List<JobDefinition> jobDefinitions = jobNodes.stream().map(JobNode::getJobDefinition).toList();
        List<Map<String, Object>> jobParameters = items.stream().map(NestedOrderItemCmd::getParameters).toList();

        Map<String, Object> args = createRuleArgs(jobParameters, jobDefinitions);

        if(Utils.isBlank(orderNo))
            orderNo = ruleGatewayService.executeNamingRule(OrderRuleTypes.ORDER_NO_RULE, args);

        if(Utils.isBlank(orderName))
            orderName = ruleGatewayService.executeNamingRule(OrderRuleTypes.ORDER_NAMING_RULE, args);

        Order order = new Order(orderNo, orderName, note, workflow);

        for (NestedOrderItemCmd item : items) {
            JobNode jobNode = jobNodes.stream().filter(
                    n -> n.getNodeKey().equals(item.getJobNodeKey())
            ).findAny().orElseThrow(
                    ()->new StratoException(
                            "Job node %s not found in workflow %s.".formatted(item.getJobNodeKey(), workflow.getName())
                    )
            );
            order.addItem(jobNode, item.getParameters());
        }

        return order;
    }
}
