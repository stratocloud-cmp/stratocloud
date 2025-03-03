package com.stratocloud.provider.script;

import com.stratocloud.provider.guest.command.GuestCommandExecutorFactory;
import com.stratocloud.provider.guest.command.GuestCommandType;
import com.stratocloud.resource.Resource;
import com.stratocloud.script.RemoteScript;
import com.stratocloud.script.RemoteScriptType;
import org.springframework.beans.factory.InitializingBean;

public interface RemoteScriptExecutor extends InitializingBean {

    ExecutionType getExecutionType();

    RemoteScriptResult execute(GuestCommandExecutorFactory<?> commandExecutorFactory,
                               Resource guestOsResource,
                               RemoteScript remoteScript);

    record ExecutionType(GuestCommandType commandType, RemoteScriptType scriptType) {}

    @Override
    default void afterPropertiesSet() throws Exception {
        RemoteScriptExecutorRegistry.register(this);
    }
}
