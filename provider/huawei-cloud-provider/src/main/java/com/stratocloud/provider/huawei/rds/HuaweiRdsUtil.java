package com.stratocloud.provider.huawei.rds;

import com.huaweicloud.sdk.rds.v3.model.GetJobInfoResponseBodyJob;
import com.huaweicloud.sdk.rds.v3.model.ListJobInfoResponse;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.job.TaskContext;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;

import java.util.Objects;
import java.util.Optional;

public class HuaweiRdsUtil {
    public static ResourceActionResult checkActionResult(Resource resource){
        Optional<String> taskId = TaskContext.getExternalTaskId();

        if(taskId.isEmpty())
            return ResourceActionResult.finished();

        HuaweiCloudProvider provider = (HuaweiCloudProvider) resource.getResourceHandler().getProvider();
        ExternalAccount account = provider.getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<ListJobInfoResponse> job = provider.buildClient(account).rds().describeJob(taskId.get());

        if(job.isEmpty() || job.get().getJob() == null)
            return ResourceActionResult.finished();

        if(Objects.equals(job.get().getJob().getStatus(), GetJobInfoResponseBodyJob.StatusEnum.RUNNING))
            return ResourceActionResult.inProgress();

        if(Objects.equals(job.get().getJob().getStatus(), GetJobInfoResponseBodyJob.StatusEnum.FAILED))
            return ResourceActionResult.failed(job.get().getJob().getFailReason());

        return ResourceActionResult.finished();
    }
}
