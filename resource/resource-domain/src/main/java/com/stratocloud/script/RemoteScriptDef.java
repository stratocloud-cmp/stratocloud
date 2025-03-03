package com.stratocloud.script;

import com.stratocloud.form.custom.CustomForm;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Optional;

@Getter
@Setter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RemoteScriptDef implements RemoteScriptHolder{
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RemoteScriptType scriptType;
    @Column(nullable = false)
    private String content;
    @Column
    private String programPath;

    @Column(columnDefinition = "TEXT")
    private String customFormJson;

    public RemoteScriptDef(RemoteScript remoteScript, CustomForm customForm){
        this.scriptType = remoteScript.type();
        this.content = remoteScript.content();
        this.programPath = remoteScript.programPath();

        if(customForm != null)
            this.customFormJson = JSON.toJsonString(customForm);
    }

    @Override
    public RemoteScript getRemoteScript() {
        return new RemoteScript(scriptType, content, programPath);
    }

    @Override
    public Optional<CustomForm> getCustomForm() {
        if(Utils.isBlank(customFormJson))
            return Optional.empty();

        return Optional.of(JSON.toJavaObject(customFormJson, CustomForm.class));
    }
}
