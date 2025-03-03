package com.stratocloud.external.resource;

import com.stratocloud.request.JobParameters;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.job.cmd.CreateJobCmd;
import com.stratocloud.job.JobApi;
import com.stratocloud.job.query.DescribeJobsRequest;
import com.stratocloud.job.query.NestedJobResponse;
import com.stratocloud.utils.JSON;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class OrderJobGatewayServiceImpl implements OrderJobGatewayService {

    private final JobApi jobApi;

    public OrderJobGatewayServiceImpl(JobApi jobApi) {
        this.jobApi = jobApi;
    }

    @Override
    public void createSingleJob(Long jobId,
                                String jobType,
                                JobParameters jobParameters,
                                String note,
                                Map<String, Object> runtimeProperties) {
        CreateJobCmd createJobCmd = new CreateJobCmd();
        createJobCmd.setJobId(jobId);
        createJobCmd.setJobType(jobType);
        createJobCmd.setJobParameters(JSON.toMap(jobParameters));
        createJobCmd.setNote(note);
        createJobCmd.setRuntimeProperties(runtimeProperties);

        jobApi.createJob(createJobCmd);
    }

    @Override
    public String getJobTypeById(Long jobId){
        DescribeJobsRequest request = new DescribeJobsRequest();
        request.setJobIds(List.of(jobId));
        Page<NestedJobResponse> page = jobApi.describeJobs(request);
        if(page.getContent().isEmpty())
            throw new StratoException("Job not found by id: %s.".formatted(jobId));
        return page.getContent().get(0).getJobType();
    }
}
