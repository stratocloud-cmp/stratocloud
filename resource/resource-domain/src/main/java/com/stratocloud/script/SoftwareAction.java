package com.stratocloud.script;

import com.stratocloud.jpa.entities.Auditable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SoftwareAction extends Auditable {

    @ManyToOne
    private SoftwareDefinition software;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SoftwareActionType actionType;
    @Column(nullable = false)
    private String actionId;
    @Column(nullable = false)
    private String actionName;

    @Embedded
    private RemoteScriptDef remoteScriptDef;

    public SoftwareAction(SoftwareActionType actionType,
                          String actionId,
                          String actionName,
                          RemoteScriptDef remoteScriptDef) {
        this.actionType = actionType;
        this.actionId = actionId;
        this.actionName = actionName;
        this.remoteScriptDef = remoteScriptDef;
    }
}
