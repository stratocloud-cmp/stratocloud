package com.stratocloud.provider.tencent.instance.command;

import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.guest.command.GuestCommandType;
import com.stratocloud.resource.Resource;

public class TencentShellCommandExecutor extends TencentCommandExecutor{
    public TencentShellCommandExecutor(Resource guestOsResource, GuestOsHandler guestOsHandler) {
        super(guestOsResource, guestOsHandler);
    }

    @Override
    protected String getTencentCommandType() {
        return "SHELL";
    }

    @Override
    public GuestCommandType getCommandType() {
        return GuestCommandType.SHELL;
    }
}
