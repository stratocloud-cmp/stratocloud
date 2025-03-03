package com.stratocloud.provider.huawei.servers.command;

import com.huaweicloud.sdk.coc.v1.model.AddScriptModel;
import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.guest.command.GuestCommandType;
import com.stratocloud.resource.Resource;


public class HuaweiShellCommandExecutor extends HuaweiCommandExecutor{
    public HuaweiShellCommandExecutor(Resource guestOsResource, GuestOsHandler guestOsHandler) {
        super(guestOsResource, guestOsHandler);
    }

    @Override
    protected AddScriptModel.TypeEnum getHuaweiScriptType() {
        return AddScriptModel.TypeEnum.SHELL;
    }

    @Override
    public GuestCommandType getCommandType() {
        return GuestCommandType.SHELL;
    }
}
