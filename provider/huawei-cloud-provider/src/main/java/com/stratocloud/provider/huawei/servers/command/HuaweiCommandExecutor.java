package com.stratocloud.provider.huawei.servers.command;

import com.huaweicloud.sdk.coc.v1.model.*;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.guest.command.GuestCommand;
import com.stratocloud.provider.guest.command.GuestCommandResult;
import com.stratocloud.provider.guest.command.ProviderGuestCommandExecutor;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.concurrent.SleepUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
public abstract class HuaweiCommandExecutor implements ProviderGuestCommandExecutor {

    private final Resource guestOsResource;

    private final GuestOsHandler guestOsHandler;

    protected HuaweiCommandExecutor(Resource guestOsResource, GuestOsHandler guestOsHandler) {
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

    protected abstract AddScriptModel.TypeEnum getHuaweiScriptType();

    @Override
    public GuestCommandResult execute(GuestCommand command) {
        HuaweiCloudProvider provider = (HuaweiCloudProvider) guestOsHandler.getProvider();
        ExternalAccount account = provider.getAccountRepository().findExternalAccount(guestOsResource.getAccountId());
        HuaweiCloudClient client = provider.buildClient(account);

        String scriptId = client.coc().createScript(
                new CreateScriptRequest().withBody(
                        new AddScriptModel()
                                .withContent(command.content())
                                .withName(UUID.randomUUID().toString())
                                .withDescription(guestOsResource.getName())
                                .withType(getHuaweiScriptType())
                                .withProperties(
                                        new ScriptPropertiesModel()
                                                .withRiskLevel(ScriptPropertiesModel.RiskLevelEnum.MEDIUM)
                                                .withVersion("1.0.0")
                                )
                )
        );

        BatchListResourceResponseData cocResource = client.coc().describeResource(guestOsResource.getExternalId()).orElseThrow(
                () -> new StratoException("Coc resource not found")
        );

        final int batchIndex = 1;

        String executionId = client.coc().executeScript(
                new ExecuteScriptRequest().withScriptUuid(scriptId).withBody(
                        new ScriptExecuteModel().withExecuteParam(
                                new ScriptExecuteParam()
                                        .withResourceful(false)
                                        .withTimeout(300)
                                        .withExecuteUser("root")
                                        .withSuccessRate(100.0)
                        ).withExecuteBatches(
                                List.of(
                                        new ExecuteInstancesBatchInfo()
                                                .withBatchIndex(batchIndex)
                                                .withRotationStrategy(
                                                        ExecuteInstancesBatchInfo.RotationStrategyEnum.CONTINUE
                                                )
                                                .withTargetInstances(
                                                        List.of(
                                                                new ExecuteResourceInstance().withType(
                                                                        ExecuteResourceInstance.TypeEnum.CLOUDSERVER
                                                                ).withAgentSn(
                                                                        cocResource.getAgentId()
                                                                ).withProjectId(
                                                                        cocResource.getProjectId()
                                                                ).withResourceId(
                                                                        cocResource.getResourceId()
                                                                ).withRegionId(
                                                                        cocResource.getRegionId()
                                                                )
                                                        )
                                                )
                                )
                        )
                )
        ).getData();

        Set<ExectionInstanceModel.StatusEnum> waitingStatuses = Set.of(
                ExectionInstanceModel.StatusEnum.READY,
                ExectionInstanceModel.StatusEnum.PROCESSING
        );

        int count = 0;
        final int maxCount = 60;
        ExectionInstanceModel execution = client.coc().describeExecutionBatch(
                executionId, batchIndex
        ).stream().findAny().orElseThrow(
                () -> new StratoException("Execution not found")
        );
        while (waitingStatuses.contains(execution.getStatus()) && count<maxCount){
            SleepUtil.sleep(5);

            execution = client.coc().describeExecutionBatch(
                    executionId, batchIndex
            ).stream().findAny().orElseThrow(
                    () -> new StratoException("Execution not found")
            );

            count++;
        }

        log.info(
                "Huawei instance command executed, instanceId={}, status={}:\nCommand:\n{}\nOutput:\n{}",
                guestOsResource.getExternalId(), execution.getStatus(), command.content(), execution.getMessage()
        );

        if(ExectionInstanceModel.StatusEnum.FINISHED.equals(execution.getStatus()))
            return GuestCommandResult.succeed(execution.getMessage(), execution.getMessage());
        else if(ExectionInstanceModel.StatusEnum.ABNORMAL.equals(execution.getStatus()))
            return GuestCommandResult.failed(execution.getMessage(), execution.getMessage());
        else
            return GuestCommandResult.failed(
                    null,
                    "Huawei coc execution is in %s status".formatted(execution.getStatus().getValue())
            );
    }

    @Override
    public void close() {

    }
}
