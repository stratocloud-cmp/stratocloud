package com.stratocloud.script.cmd;

import com.stratocloud.exceptions.InvalidArgumentException;
import com.stratocloud.form.custom.CustomForm;
import com.stratocloud.script.RemoteScript;
import com.stratocloud.utils.Assert;
import com.stratocloud.utils.JSON;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Data
@NoArgsConstructor
public class NestedRemoteScriptDef {
    private RemoteScript remoteScript;

    @Setter(AccessLevel.NONE)
    private CustomForm customForm;
    @Setter(AccessLevel.NONE)
    private Map<String, Object> customFormMetaData;

    public void validate(){
        Assert.isNotNull(remoteScript);

        if(remoteScript.type() == null)
            throw new InvalidArgumentException("脚本类型不能为空");

        if(remoteScript.content() == null)
            throw new InvalidArgumentException("脚本不能为空");

    }

    public void setCustomForm(CustomForm customForm) {
        this.customForm = customForm;

        if(customForm != null)
            this.customFormMetaData = JSON.toMap(customForm.toDynamicFormMetaData());
    }
}
