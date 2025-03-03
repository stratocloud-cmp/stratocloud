package com.stratocloud.script;

import com.stratocloud.form.custom.CustomForm;

import java.util.Optional;

public interface RemoteScriptHolder {
    RemoteScript getRemoteScript();

    Optional<CustomForm> getCustomForm();
}
