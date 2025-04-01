package com.stratocloud.script;

import com.stratocloud.jpa.entities.Auditable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SoftwareRequirement extends Auditable {

    @ManyToOne
    private SoftwareDefinition target;

    @ManyToOne
    private SoftwareDefinition source;

    @Column(nullable = false)
    private String requirementKey;
    @Column(nullable = false)
    private String requirementName;
    @Column(nullable = false)
    private String capabilityName;

    @Column(nullable = false)
    private boolean exclusive;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "scriptType", column = @Column(name = "connect_script_type", nullable = false)),
            @AttributeOverride(name = "content", column = @Column(name = "connect_script_content", nullable = false, columnDefinition = "TEXT")),
            @AttributeOverride(name = "programPath", column = @Column(name = "connect_script_program_path")),
            @AttributeOverride(name = "customFormJson", column = @Column(name = "connect_script_custom_form_json", columnDefinition = "TEXT"))
    })
    private RemoteScriptDef connectScriptDef;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "scriptType", column = @Column(name = "disconnect_script_type", nullable = false)),
            @AttributeOverride(name = "content", column = @Column(name = "disconnect_script_content", nullable = false, columnDefinition = "TEXT")),
            @AttributeOverride(name = "programPath", column = @Column(name = "disconnect_script_program_path")),
            @AttributeOverride(name = "customFormJson", column = @Column(name = "disconnect_script_custom_form_json", columnDefinition = "TEXT"))
    })
    private RemoteScriptDef disconnectScriptDef;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "scriptType", column = @Column(name = "check_script_type", nullable = false)),
            @AttributeOverride(name = "content", column = @Column(name = "check_script_content", nullable = false, columnDefinition = "TEXT")),
            @AttributeOverride(name = "programPath", column = @Column(name = "check_script_program_path")),
            @AttributeOverride(name = "customFormJson", column = @Column(name = "check_script_custom_form_json", columnDefinition = "TEXT"))
    })
    private RemoteScriptDef checkConnectionScriptDef;


    public SoftwareRequirement(SoftwareDefinition target,
                               String requirementKey,
                               String requirementName,
                               String capabilityName,
                               boolean exclusive,
                               RemoteScriptDef connectScriptDef,
                               RemoteScriptDef disconnectScriptDef,
                               RemoteScriptDef checkConnectionScriptDef) {
        this.target = target;
        this.requirementKey = requirementKey;
        this.requirementName = requirementName;
        this.capabilityName = capabilityName;
        this.exclusive = exclusive;
        this.connectScriptDef = connectScriptDef;
        this.disconnectScriptDef = disconnectScriptDef;
        this.checkConnectionScriptDef = checkConnectionScriptDef;
    }
}
