package com.stratocloud.provider.tencent.instance.command;


import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.guest.command.GuestCommandType;
import com.stratocloud.resource.Resource;

public class TencentPowerShellCommandExecutor extends TencentCommandExecutor{
    public TencentPowerShellCommandExecutor(Resource guestOsResource, GuestOsHandler guestOsHandler) {
        super(guestOsResource, guestOsHandler);
    }

    @Override
    protected String getTencentCommandType() {
        return "";
    }

    @Override
    public GuestCommandType getCommandType() {
        return null;
    }
}
