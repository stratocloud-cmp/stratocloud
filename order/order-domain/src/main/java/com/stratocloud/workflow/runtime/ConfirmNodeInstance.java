package com.stratocloud.workflow.runtime;

import com.stratocloud.messaging.Message;
import com.stratocloud.workflow.messaging.WorkflowReportConfirmStartedPayload;
import com.stratocloud.workflow.messaging.WorkflowTopics;
import com.stratocloud.workflow.nodes.ConfirmNode;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConfirmNodeInstance extends NodeInstance {

    public ConfirmNodeInstance(ConfirmNode confirmNode) {
        super(confirmNode);
    }

    @Override
    protected void start() {
        super.start();

        ConfirmNode confirmNode = (ConfirmNode) getNode();

        Message message = Message.create(
                WorkflowTopics.WORKFLOW_REPORT_CONFIRM_STARTED,
                new WorkflowReportConfirmStartedPayload(getId(), confirmNode.getPossibleHandlers())
        );

        publish(message);
    }
}
