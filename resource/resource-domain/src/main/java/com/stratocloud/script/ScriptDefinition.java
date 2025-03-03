package com.stratocloud.script;

import com.stratocloud.jpa.entities.Controllable;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScriptDefinition extends Controllable {
    @Column(nullable = false)
    private String definitionKey;
    @Column(nullable = false)
    private String name;
    @Column
    private String description;
    @Column(nullable = false)
    private boolean publicDefinition;
    @Column(nullable = false)
    private boolean visibleInTarget;
    @Column(nullable = false)
    private boolean disabled;

    @Embedded
    private RemoteScriptDef remoteScriptDef;

    public ScriptDefinition(String definitionKey,
                            String name,
                            String description,
                            boolean publicDefinition,
                            boolean visibleInTarget,
                            RemoteScriptDef remoteScriptDef) {
        this.definitionKey = definitionKey;
        this.name = name;
        this.description = description;
        this.publicDefinition = publicDefinition;
        this.visibleInTarget = visibleInTarget;
        this.remoteScriptDef = remoteScriptDef;
    }


    public void update(String name,
                       String description,
                       boolean publicDefinition,
                       boolean visibleInTarget,
                       RemoteScriptDef remoteScriptDef) {
        this.name = name;
        this.description = description;
        this.publicDefinition = publicDefinition;
        this.visibleInTarget = visibleInTarget;
        this.remoteScriptDef = remoteScriptDef;
    }

    public void enable(){
        this.disabled = false;
    }

    public void disable(){
        this.disabled = true;
    }
}
