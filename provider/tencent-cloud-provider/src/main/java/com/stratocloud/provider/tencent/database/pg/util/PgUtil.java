package com.stratocloud.provider.tencent.database.pg.util;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.job.TaskContext;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.database.pg.TencentPgHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import com.stratocloud.resource.ResourceCost;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.postgres.v20170312.models.DBInstance;
import com.tencentcloudapi.postgres.v20170312.models.InquiryPriceCreateDBInstancesRequest;
import com.tencentcloudapi.postgres.v20170312.models.TaskDetail;
import com.tencentcloudapi.postgres.v20170312.models.Version;
import lombok.extern.slf4j.Slf4j;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class PgUtil {
    public static boolean isPrepaid(DBInstance pg) {
        return Objects.equals(pg.getPayType(), "prepaid");
    }

    public static ResourceCost getPgInstanceCost(Resource resource, long prepaidPeriod) {
        TencentPgHandler pgHandler = (TencentPgHandler) resource.getResourceHandler();
        ExternalAccount account = pgHandler.getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) pgHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        Optional<DBInstance> instance = pgHandler.describePg(account, resource.getExternalId());

        if(instance.isEmpty())
            return ResourceCost.ZERO;

        DBInstance pg = instance.get();

        InquiryPriceCreateDBInstancesRequest request = new InquiryPriceCreateDBInstancesRequest();

        double timeAmount;
        ChronoUnit timeUnit;

        if(isPrepaid(pg)){
            request.setInstanceChargeType("PREPAID");
            request.setPeriod(prepaidPeriod);

            timeAmount = prepaidPeriod;
            timeUnit = ChronoUnit.MONTHS;
        } else {
            request.setInstanceChargeType("POSTPAID");
            request.setPeriod(1L);

            timeAmount = 1;
            timeUnit = ChronoUnit.HOURS;
        }

        request.setInstanceCount(1L);

        request.setZone(pg.getZone());
        request.setSpecCode(pg.getDBInstanceClass());
        request.setDBEngine(pg.getDBEngine());
        request.setStorage(pg.getDBInstanceStorage());
        request.setInstanceType(pg.getDBInstanceType());

        var response = client.describePgPrice(request);

        Long price = response.getPrice();

        if(price == null)
            return ResourceCost.ZERO;

        return new ResourceCost(price / 100.0, timeAmount, timeUnit);
    }

    public static ResourceActionResult checkTaskResult(Resource resource) {
        Optional<String> taskId = TaskContext.getExternalTaskId();
        if(taskId.isEmpty())
            return ResourceActionResult.finished();

        ResourceHandler resourceHandler = resource.getResourceHandler();
        TencentCloudProvider provider = (TencentCloudProvider) resourceHandler.getProvider();
        ExternalAccount account = resourceHandler.getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        long pgTaskId;

        try {
            pgTaskId = Long.parseLong(taskId.get());
        }catch (Exception e){
            log.warn("Unexpected pg task id: {}", taskId.get());
            return ResourceActionResult.finished();
        }

        var pgTask = client.describePgTask(pgTaskId);

        if(pgTask.isEmpty() || pgTask.get().getStatus() == null)
            return ResourceActionResult.finished();

        TaskDetail taskDetail = pgTask.get().getTaskDetail();

        return switch (pgTask.get().getStatus()) {
            case "Fail" -> ResourceActionResult.failed(
                    taskDetail != null ? taskDetail.getMessage() : pgTask.get().getStatus()
            );
            case "Running" -> ResourceActionResult.inProgress();
            default -> ResourceActionResult.finished();
        };
    }

    public static String getInstanceClassName(PgInstanceClass instanceClass) {
        return "%s (CPU:%s核 内存:%sGB 最大QPS:%s)".formatted(
                instanceClass.detail().getSpecCode(),
                instanceClass.detail().getCPU(),
                instanceClass.detail().getMemory() / 1024,
                instanceClass.detail().getQPS()
        );
    }


    public static PgUpgradeTargets getUpgradeTargets(Resource pgResource){
        List<String> minorUpgradeTargets = new ArrayList<>();
        List<String> majorUpgradeTargets = new ArrayList<>();

        if(Utils.isBlank(pgResource.getExternalId()))
            return new PgUpgradeTargets("Unknown", minorUpgradeTargets, majorUpgradeTargets);

        ResourceHandler resourceHandler = pgResource.getResourceHandler();
        TencentCloudProvider provider = (TencentCloudProvider) resourceHandler.getProvider();
        ExternalAccount account = provider.getAccountRepository().findExternalAccount(pgResource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        Map<String, Version> versionMap = client.describePgVersions().stream().collect(
                Collectors.toMap(Version::getDBKernelVersion, v -> v)
        );

        Optional<DBInstance> pg = client.describePgInstance(pgResource.getExternalId());
        if(pg.isEmpty())
            return new PgUpgradeTargets("Unknown", minorUpgradeTargets, majorUpgradeTargets);

        Version version = versionMap.get(pg.get().getDBKernelVersion());

        if(version == null || Utils.isEmpty(version.getAvailableUpgradeTarget()))
            return new PgUpgradeTargets(pg.get().getDBKernelVersion(), minorUpgradeTargets, majorUpgradeTargets);

        for (String target : version.getAvailableUpgradeTarget()) {
            Version targetVersion = versionMap.get(target);

            if(Objects.equals(version.getDBMajorVersion(), targetVersion.getDBMajorVersion()))
                minorUpgradeTargets.add(target);
            else
                majorUpgradeTargets.add(target);
        }

        return new PgUpgradeTargets(version.getDBKernelVersion(), minorUpgradeTargets, majorUpgradeTargets);
    }
}
