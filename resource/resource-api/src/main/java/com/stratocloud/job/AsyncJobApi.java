package com.stratocloud.job;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.job.cmd.RunAsyncJobCmd;
import com.stratocloud.job.query.DescribeAsyncJobsRequest;
import com.stratocloud.job.query.NestedAsyncJobResponse;
import com.stratocloud.job.response.RunAsyncJobResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface AsyncJobApi {
    @PostMapping(StratoServices.RESOURCE_SERVICE+"/run-async-job")
    RunAsyncJobResponse runAsyncJob(@RequestBody RunAsyncJobCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-async-jobs")
    Page<NestedAsyncJobResponse> describeAsyncJobs(@RequestBody DescribeAsyncJobsRequest request);
}
