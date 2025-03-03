package com.stratocloud.provider.guest.command;

import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.resource.Resource;

import java.io.Closeable;

public interface GuestCommandExecutor extends Closeable {
    Resource getGuestOsResource();

    GuestOsHandler getGuestOsHandler();

    GuestCommandType getCommandType();

    GuestCommandResult execute(GuestCommand command);
}
