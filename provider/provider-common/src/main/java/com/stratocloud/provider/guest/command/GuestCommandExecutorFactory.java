package com.stratocloud.provider.guest.command;

import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.resource.Resource;

public interface GuestCommandExecutorFactory<E extends GuestCommandExecutor>{
    GuestCommandType getCommandType();

    E createExecutor(GuestOsHandler guestOsHandler, Resource resource);
}
