package com.stratocloud.resource;

import com.stratocloud.job.Task;
import com.stratocloud.job.TaskTargetEntity;
import com.stratocloud.jpa.entities.Auditable;
import com.stratocloud.provider.relationship.DependsOnRelationshipHandler;
import com.stratocloud.provider.relationship.PrimaryCapabilityHandler;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.task.ConnectTaskHandler;
import com.stratocloud.resource.task.DisconnectTaskHandler;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Getter
@Setter
@Entity
@Table(indexes = {
        @Index(name = "source_idx", columnList = "source_id"),
        @Index(name = "source_idx", columnList = "type"),
        @Index(name = "target_idx", columnList = "target_id"),
        @Index(name = "target_idx", columnList = "type"),
})
public class Relationship extends Auditable implements TaskTargetEntity {
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String typeName;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private Resource source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id")
    private Resource target;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RelationshipState state = RelationshipState.NO_STATE;

    @Column
    private String errorMessage;

    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> properties;


    @Override
    public String getEntityDescription() {
        return "%s: %s->%s".formatted(typeName, source.getName(), target.getName());
    }

    public Task createConnectTask(){
        ConnectTaskInputs connectTaskInputs = new ConnectTaskInputs(properties);
        return new Task(this, ConnectTaskHandler.TASK_TYPE, connectTaskInputs);
    }


    public RelationshipHandler getHandler() {
        if(isSimpleDependsOn())
            return new DependsOnRelationshipHandler(source.getResourceHandler(), target.getResourceHandler());

        ResourceHandler resourceHandler = target.getResourceHandler();
        return resourceHandler.getCapability(type);
    }

    public boolean isSimpleDependsOn() {
        return DependsOnRelationshipHandler.TYPE_ID.equals(type);
    }


    public void onConnected() {
        this.state = RelationshipState.CONNECTED;
    }

    public void onConnecting(){
        this.state = RelationshipState.CONNECTING;
    }

    public void onDisconnecting(){
        this.state = RelationshipState.DISCONNECTING;
    }


    public void onConnectionFailed(String errorMessage) {
        this.state = RelationshipState.ERROR;
        this.errorMessage = errorMessage;
    }

    public void onDisconnectionFailed(String errorMessage) {
        this.state = RelationshipState.ERROR;
        this.errorMessage = errorMessage;
    }

    public Task createDisconnectTask() {
        DisconnectTaskInputs disconnectTaskInputs = new DisconnectTaskInputs();
        return new Task(this, DisconnectTaskHandler.TASK_TYPE, disconnectTaskInputs);
    }

    public void onDisconnected() {
        this.state = RelationshipState.DISCONNECTED;
    }

    public void onLost(){
        if(this.state != RelationshipState.CONNECTED)
            return;

        this.state = RelationshipState.LOST;
    }


    public boolean isPrimaryCapability() {
        return getHandler() instanceof PrimaryCapabilityHandler;
    }
}
