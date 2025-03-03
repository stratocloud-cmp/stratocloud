package com.stratocloud.order;

import com.stratocloud.external.order.JobHandlerGatewayService;
import com.stratocloud.jpa.entities.Auditable;
import com.stratocloud.utils.ContextUtil;
import com.stratocloud.workflow.nodes.JobNode;
import com.stratocloud.workflow.runtime.JobNodeInstance;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Slf4j
@Entity
@Getter
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends Auditable {

    private static final int MAX_SUMMARY_SIZE = 50;

    @ManyToOne
    private Order order;
    @ManyToOne
    private JobNode jobNode;
    @OneToOne(cascade = CascadeType.ALL)
    private JobNodeInstance jobNodeInstance;
    @Column
    private Map<String, Object> parameters;

    public OrderItem(Order order, JobNode jobNode, Map<String, Object> parameters) {
        this.order = order;
        this.jobNode = jobNode;
        this.parameters = parameters;
    }

    public void attachJobNodeInstance(JobNodeInstance jobNodeInstance){
        jobNodeInstance.getJob().setParameters(parameters);
        this.jobNodeInstance = jobNodeInstance;
    }

    public void updateParameters(Map<String, Object> parameters) {
        if(jobNodeInstance!=null)
            jobNodeInstance.getJob().setParameters(parameters);
        this.parameters = parameters;
    }

    public List<String> collectSummaryData() {
        JobHandlerGatewayService jobHandlerGatewayService = ContextUtil.getBean(JobHandlerGatewayService.class);
        List<String> details = jobHandlerGatewayService.collectSummaryData(jobNodeInstance.getJob());
        if(details.size() > MAX_SUMMARY_SIZE){
            log.warn("Job summary details is too long, cutting down to {}...", MAX_SUMMARY_SIZE);
            details = new ArrayList<>(details.subList(0, MAX_SUMMARY_SIZE));
            details.add("...");
        }

        return details;
    }

    public void detachNodeInstance() {
        this.jobNodeInstance = null;
    }
}
