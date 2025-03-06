package com.stratocloud.provider.tencent.instance.command;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.guest.command.GuestCommand;
import com.stratocloud.provider.guest.command.GuestCommandResult;
import com.stratocloud.provider.guest.command.ProviderGuestCommandExecutor;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.SecurityUtil;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.tat.v20201028.models.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;

@SuppressWarnings("ALL")
@Slf4j
public abstract class TencentCommandExecutor implements ProviderGuestCommandExecutor {

    private final Resource guestOsResource;

    private final GuestOsHandler guestOsHandler;

    private final long timeoutSeconds = 60L;

    public TencentCommandExecutor(Resource guestOsResource, GuestOsHandler guestOsHandler) {
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

    protected abstract String getTencentCommandType();

    @Override
    public GuestCommandResult execute(GuestCommand command) {
        TencentCloudProvider provider = (TencentCloudProvider) guestOsHandler.getProvider();
        ExternalAccount account = provider.getAccountRepository().findExternalAccount(guestOsResource.getAccountId());
        RunCommandRequest request = new RunCommandRequest();
        request.setContent(SecurityUtil.encodeToBase64(command.content()));
        request.setInstanceIds(new String[]{guestOsResource.getExternalId()});
        request.setCommandType(getTencentCommandType());
        request.setTimeout(timeoutSeconds);

        Invocation invocation = provider.buildClient(account).runCommand(request);

        Set<String> successStatuses = Set.of("SUCCESS");
        Set<String> failedStatuses = Set.of(
                "DELIVER_FAILED", "START_FAILED", "FAILED", "TIMEOUT",
                "TASK_TIMEOUT", "CANCELLING", "CANCELLED", "TERMINATED"
        );

        InvocationTaskBasicInfo[] taskBasicInfoSet = invocation.getInvocationTaskBasicInfoSet();
        if(Utils.isEmpty(taskBasicInfoSet))
            return GuestCommandResult.failed(null, "Invocation task not found.");

        String taskId = taskBasicInfoSet[0].getInvocationTaskId();

        Optional<InvocationTask> invocationTask = provider.buildClient(account).describeInvocationTask(taskId);

        if(invocationTask.isEmpty())
            return GuestCommandResult.failed(null, "Invocation task not found.");

        TaskResult taskResult = invocationTask.get().getTaskResult();
        String taskStatus = invocationTask.get().getTaskStatus();
        String output = SecurityUtil.decodeFromBase64(taskResult.getOutput());
        String errorInfo = invocationTask.get().getErrorInfo();
        String realError = Utils.isBlank(errorInfo) ? output : errorInfo;

        log.info(
                "Tencent instance command executed, instanceId={}, status={}, exitCode={}:\nCommand:\n{}\nOutput:\n{}\nError:\n{}",
                guestOsResource.getExternalId(), taskStatus, taskResult.getExitCode(), command.content(), output, errorInfo
        );

        if(successStatuses.contains(taskStatus))
            return GuestCommandResult.succeed(output, errorInfo);
        else if(failedStatuses.contains(taskStatus))
            return GuestCommandResult.failed(output, realError);
        else
            return GuestCommandResult.failed(output, "Unexpected invocation task status: %s".formatted(taskStatus));
    }

    @Override
    public void close() {

    }
}
