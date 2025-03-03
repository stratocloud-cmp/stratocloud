package com.stratocloud.provider.aliyun.instance.command;

import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.guest.command.GuestCommandType;
import com.stratocloud.resource.Resource;

public class AliyunShellCommandExecutor extends AliyunCommandExecutor{

    public AliyunShellCommandExecutor(Resource guestOsResource, GuestOsHandler guestOsHandler) {
        super(guestOsResource, guestOsHandler);
    }

    @Override
    protected String getAliyunCommandType() {
        return "RunShellScript";
    }

    @Override
    public GuestCommandType getCommandType() {
        return GuestCommandType.SHELL;
    }
}
