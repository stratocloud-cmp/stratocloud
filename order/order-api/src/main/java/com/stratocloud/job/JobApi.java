package com.stratocloud.job;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.job.cmd.*;
import com.stratocloud.job.query.*;
import com.stratocloud.job.response.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface JobApi {
    @PostMapping(StratoServices.ORDER_SERVICE+"/create-job")
    CreateJobResponse createJob(@RequestBody CreateJobCmd cmd);

    @PostMapping(StratoServices.ORDER_SERVICE + "/describe-jobs")
    Page<NestedJobResponse> describeJobs(@RequestBody DescribeJobsRequest request);

    @PostMapping(StratoServices.ORDER_SERVICE + "/describe-job-definitions")
    Page<NestedJobDefinitionResponse> describeJobDefinitions(@RequestBody DescribeJobDefinitionsRequest request);

    @PostMapping(StratoServices.ORDER_SERVICE + "/change-job-definition-order-requirement")
    ChangeOrderRequirementResponse changeJobDefinitionOrderRequirement(@RequestBody ChangeOrderRequirementCmd cmd);


    @PostMapping(StratoServices.ORDER_SERVICE + "/describe-job-triggers")
    Page<NestedJobTriggerResponse> describeJobTriggers(@RequestBody DescribeJobTriggersRequest request);

    @PostMapping(StratoServices.ORDER_SERVICE + "/update-job-trigger")
    UpdateJobTriggerResponse updateJobTrigger(@RequestBody UpdateJobTriggerCmd cmd);

    @PostMapping(StratoServices.ORDER_SERVICE + "/disable-job-trigger")
    DisableJobTriggerResponse disableTrigger(@RequestBody DisableJobTriggerCmd cmd);

    @PostMapping(StratoServices.ORDER_SERVICE + "/enable-job-trigger")
    EnableJobTriggerResponse enableTrigger(@RequestBody EnableJobTriggerCmd cmd);

    @PostMapping(StratoServices.ORDER_SERVICE + "/trigger-job-once")
    TriggerJobOnceResponse triggerJobOnce(@RequestBody TriggerJobOnceCmd cmd);

    @PostMapping(StratoServices.ORDER_SERVICE + "/retry-job")
    RetryJobResponse retryJob(@RequestBody RetryJobCmd cmd);
}
