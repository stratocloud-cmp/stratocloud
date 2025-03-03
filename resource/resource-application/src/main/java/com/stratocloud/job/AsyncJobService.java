package com.stratocloud.job;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.job.cmd.RunAsyncJobCmd;
import com.stratocloud.job.query.DescribeAsyncJobsRequest;
import com.stratocloud.job.query.NestedAsyncJobResponse;
import com.stratocloud.job.response.RunAsyncJobResponse;
import org.springframework.data.domain.Page;

public interface AsyncJobService {
    RunAsyncJobResponse runAsyncJob(RunAsyncJobCmd cmd);

    Page<NestedAsyncJobResponse> describeAsyncJobs(DescribeAsyncJobsRequest request);

    static AsyncJobHandler<?> getAsyncJobHandler(AsyncJob asyncJob) {
        JobHandler<?> handler = asyncJob.getHandler();
        if(handler instanceof AsyncJobHandler<?> asyncJobHandler)
            return asyncJobHandler;
        throw new StratoException("JobHandler %s is not a AsyncJobHandler.".formatted(handler.getJobType()));
    }
}
