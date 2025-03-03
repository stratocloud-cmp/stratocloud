package com.stratocloud.job;

import com.stratocloud.job.cmd.*;
import com.stratocloud.job.query.*;
import com.stratocloud.job.response.*;
import org.springframework.data.domain.Page;

public interface JobService {
    CreateJobResponse createJob(CreateJobCmd cmd);

    Page<NestedJobResponse> describeJobs(DescribeJobsRequest request);

    Page<NestedJobDefinitionResponse> describeJobDefinitions(DescribeJobDefinitionsRequest request);

    ChangeOrderRequirementResponse changeJobDefinitionOrderRequirement(ChangeOrderRequirementCmd cmd);

    Page<NestedJobTriggerResponse> describeJobTriggers(DescribeJobTriggersRequest request);

    UpdateJobTriggerResponse updateJobTrigger(UpdateJobTriggerCmd cmd);

    EnableJobTriggerResponse enableTrigger(EnableJobTriggerCmd cmd);

    DisableJobTriggerResponse disableTrigger(DisableJobTriggerCmd cmd);

    CreateJobCmd createCmdForScheduledJob(JobDefinition jobDefinition);

    TriggerJobOnceResponse triggerJobOnce(TriggerJobOnceCmd cmd);

    RetryJobResponse retryJob(RetryJobCmd cmd);
}
