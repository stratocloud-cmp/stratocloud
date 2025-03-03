package com.stratocloud.provider.aliyun.instance.command;

import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.guest.command.GuestCommandType;
import com.stratocloud.resource.Resource;

public class AliyunPowerShellCommandExecutor extends AliyunCommandExecutor{

    public AliyunPowerShellCommandExecutor(Resource guestOsResource, GuestOsHandler guestOsHandler) {
        super(guestOsResource, guestOsHandler);
    }

    @Override
    protected String getAliyunCommandType() {
        return "RunPowerShellScript";
    }

    @Override
    public GuestCommandType getCommandType() {
        return GuestCommandType.POWERSHELL;
    }
}
