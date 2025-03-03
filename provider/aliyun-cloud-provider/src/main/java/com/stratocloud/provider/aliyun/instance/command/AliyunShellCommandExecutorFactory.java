package com.stratocloud.provider.aliyun.instance.command;

import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.guest.command.GuestCommandType;
import com.stratocloud.provider.guest.command.ProviderGuestCommandExecutor;
import com.stratocloud.provider.guest.command.ProviderGuestCommandExecutorFactory;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

@Component
public class AliyunShellCommandExecutorFactory implements ProviderGuestCommandExecutorFactory {
    @Override
    public GuestCommandType getCommandType() {
        return GuestCommandType.SHELL;
    }

    @Override
    public ProviderGuestCommandExecutor createExecutor(GuestOsHandler guestOsHandler, Resource resource) {
        return new AliyunShellCommandExecutor(resource, guestOsHandler);
    }
}
