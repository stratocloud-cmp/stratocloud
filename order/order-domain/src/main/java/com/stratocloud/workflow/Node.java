package com.stratocloud.workflow;

import com.stratocloud.jpa.entities.Auditable;
import com.stratocloud.utils.Assert;
import com.stratocloud.workflow.runtime.NodeInstance;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Node extends Auditable {
    @Column(nullable = false)
    private String nodeType;
    @Column(nullable = false)
    private String nodeKey;
    @Column(nullable = false)
    private String name;
    @ManyToOne
    private Workflow workflow;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "target", orphanRemoval = true)
    private List<SequenceFlow> incomingFlows = new ArrayList<>();
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "source", orphanRemoval = true)
    private List<SequenceFlow> outgoingFlows = new ArrayList<>();


    public abstract NodeInstance createInstance();

    public List<Node> getFromNodes() {
        return incomingFlows.stream().map(SequenceFlow::getSource).toList();
    }

    public List<Node> getToNodes() {
        return outgoingFlows.stream().map(SequenceFlow::getTarget).toList();
    }

    public void connectTo(Node target) {
        SequenceFlow sequenceFlow = new SequenceFlow(this, target);

        addOutgoingFlow(sequenceFlow);
        target.addIncomingFlow(sequenceFlow);
    }

    private void addIncomingFlow(SequenceFlow incomingFlow) {
        this.incomingFlows.add(incomingFlow);
    }

    private void addOutgoingFlow(SequenceFlow outgoingFlow) {
        this.outgoingFlows.add(outgoingFlow);
    }

    public abstract NodeProperties getProperties();

    public void validate(){
        Assert.nonBlank(nodeType, nodeKey, name);
    }

    public boolean requireOrder(){
        return false;
    }
}
