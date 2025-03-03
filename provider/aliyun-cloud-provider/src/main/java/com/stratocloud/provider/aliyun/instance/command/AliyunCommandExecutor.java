package com.stratocloud.provider.aliyun.instance.command;

import com.aliyun.ecs20140526.models.RunCommandRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.guest.command.GuestCommand;
import com.stratocloud.provider.guest.command.GuestCommandResult;
import com.stratocloud.provider.guest.command.ProviderGuestCommandExecutor;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.SecurityUtil;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Slf4j
public abstract class AliyunCommandExecutor implements ProviderGuestCommandExecutor {

    private final Resource guestOsResource;

    private final GuestOsHandler guestOsHandler;

    public AliyunCommandExecutor(Resource guestOsResource, GuestOsHandler guestOsHandler) {
        this.guestOsResource = guestOsResource;
        this.guestOsHandler = guestOsHandler;
    }

    @Override
    public Resource getGuestOsResource() {
        return guestOsResource;
    }

    @Override
    public GuestOsHandler getGuestOsHandler() {
        return guestOsHandler;
    }

    protected abstract String getAliyunCommandType();

    @Override
    public GuestCommandResult execute(GuestCommand command) {
        AliyunCloudProvider provider = (AliyunCloudProvider) guestOsHandler.getProvider();
        ExternalAccount account = provider.getAccountRepository().findExternalAccount(guestOsResource.getAccountId());

        RunCommandRequest request = new RunCommandRequest();
        request.setCommandContent(SecurityUtil.encodeToBase64(command.content()));
        request.setInstanceId(List.of(guestOsResource.getExternalId()));
        request.setType(getAliyunCommandType());
        request.setContentEncoding("Base64");

        AliyunInvocation invocation = provider.buildClient(account).ecs().runCommand(request);

        if(invocation.detail().getInvokeInstances() == null
                || Utils.isEmpty(invocation.detail().getInvokeInstances().getInvokeInstance()))
            return GuestCommandResult.failed(null, "Invocation task not found.");

        var invokeInstance = invocation.detail().getInvokeInstances().getInvokeInstance().get(0);

        Set<String> successStatuses = Set.of("Success");
        Set<String> failedStatuses = Set.of(
                "Invalid","Aborted","Failed","Error","Timeout","Cancelled","Stopping","Terminated"
        );

        String invocationStatus = invokeInstance.getInvocationStatus();
        String output = SecurityUtil.decodeFromBase64(invokeInstance.getOutput());
        String realError = Utils.isNotBlank(invokeInstance.getErrorInfo()) ?
                (output+invokeInstance.getErrorInfo()) : output;

        log.info(
                "Aliyun instance command executed, instanceId={}, status={}, exitCode={}, errorCode={}:\nCommand:\n{}\nOutput:\n{}\nError:\n{}",
                guestOsResource.getExternalId(),
                invocationStatus,
                invokeInstance.getExitCode(),
                invokeInstance.getExitCode(),
                command.content(),
                output,
                invokeInstance.getErrorInfo()
        );

        if(successStatuses.contains(invocationStatus))
            return GuestCommandResult.succeed(output, realError);
        else if(failedStatuses.contains(invocationStatus))
            return GuestCommandResult.failed(output, realError);
        else
            return GuestCommandResult.failed(
                    output,
                    "Unexpected invocation task status: %s".formatted(invocationStatus)
            );
    }

    @Override
    public void close() throws IOException {

    }
}
