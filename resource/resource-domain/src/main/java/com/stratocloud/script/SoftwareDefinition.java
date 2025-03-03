package com.stratocloud.script;

import com.stratocloud.jpa.entities.Controllable;
import com.stratocloud.resource.OsType;
import com.stratocloud.utils.Utils;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SoftwareDefinition extends Controllable {
    @Column(nullable = false)
    private String definitionKey;
    @Column(nullable = false)
    private String name;
    @Column
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SoftwareType softwareType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OsType osType;

    @Column(nullable = false)
    private boolean publicDefinition;

    @Column(nullable = false)
    private boolean visibleInTarget;

    @Column(nullable = false)
    private boolean disabled;

    @Column(nullable = false)
    private Integer servicePort;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "software", orphanRemoval = true, fetch = FetchType.EAGER)
    private List<SoftwareAction> actions = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "source", orphanRemoval = true, fetch = FetchType.EAGER)
    private List<SoftwareRequirement> requirements = new ArrayList<>();

    public SoftwareDefinition(String definitionKey,
                              String name,
                              String description,
                              SoftwareType softwareType,
                              OsType osType,
                              boolean publicDefinition,
                              boolean visibleInTarget,
                              Integer servicePort) {
        this.definitionKey = definitionKey;
        this.name = name;
        this.description = description;
        this.softwareType = softwareType;
        this.osType = osType;
        this.publicDefinition = publicDefinition;
        this.visibleInTarget = visibleInTarget;
        this.servicePort = servicePort;
    }

    public Optional<SoftwareAction> getActionByType(SoftwareActionType actionType) {
        return actions.stream().filter(sa -> actionType == sa.getActionType()).findAny();
    }

    public void updateActions(List<SoftwareAction> actions){
        this.actions.clear();

        if(Utils.isNotEmpty(actions))
            actions.forEach(this::addAction);
    }

    private void addAction(SoftwareAction softwareAction) {
        softwareAction.setSoftware(this);
        this.actions.add(softwareAction);
    }

    public void updateRequirements(List<SoftwareRequirement> requirements){
        this.requirements.clear();

        if(Utils.isNotEmpty(requirements))
            requirements.forEach(this::addRequirement);
    }

    private void addRequirement(SoftwareRequirement requirement) {
        requirement.setSource(this);
        this.requirements.add(requirement);
    }

    public void update(String name,
                       String description,
                       SoftwareType softwareType,
                       OsType osType,
                       boolean publicDefinition,
                       boolean visibleInTarget,
                       Integer servicePort) {
        this.name = name;
        this.description = description;
        this.softwareType = softwareType;
        this.osType = osType;
        this.publicDefinition = publicDefinition;
        this.visibleInTarget = visibleInTarget;
        this.servicePort = servicePort;
    }


    public String generateSoftwareResourceTypeId(String providerId){
        return providerId+"_SOFTWARE_"+definitionKey;
    }

    public void enable(){
        this.disabled = false;
    }

    public void disable(){
        this.disabled = true;
    }
}
