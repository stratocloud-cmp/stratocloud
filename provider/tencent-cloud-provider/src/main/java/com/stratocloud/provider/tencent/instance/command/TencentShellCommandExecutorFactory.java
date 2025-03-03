package com.stratocloud.provider.tencent.instance.command;

import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.guest.command.GuestCommandType;
import com.stratocloud.provider.guest.command.ProviderGuestCommandExecutor;
import com.stratocloud.provider.guest.command.ProviderGuestCommandExecutorFactory;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

@Component
public class TencentShellCommandExecutorFactory implements ProviderGuestCommandExecutorFactory {

    @Override
    public GuestCommandType getCommandType() {
        return GuestCommandType.SHELL;
    }

    @Override
    public ProviderGuestCommandExecutor createExecutor(GuestOsHandler guestOsHandler, Resource resource) {
        return new TencentShellCommandExecutor(resource, guestOsHandler);
    }
}
