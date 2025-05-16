package com.stratocloud.external.order;

import com.stratocloud.config.MonolithOnly;
import com.stratocloud.request.JobParameters;
import com.stratocloud.job.Job;
import com.stratocloud.job.JobContext;
import com.stratocloud.job.JobHandler;
import com.stratocloud.job.JobHandlerRegistry;
import com.stratocloud.utils.JSON;
import com.stratocloud.workflow.runtime.WorkflowInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@MonolithOnly
public class JobHandlerGatewayServiceImpl implements JobHandlerGatewayService {
    @Override
    @SuppressWarnings("unchecked")
    public void notifyJobUpdated(Job job) {
        var jobHandler = (JobHandler<JobParameters>) JobHandlerRegistry.getJobHandler(
                job.getJobDefinition().getJobType()
        );

        Class<?> argumentClass = jobHandler.getParameterClass();
        JobParameters jobParameters = (JobParameters) JSON.convert(job.getParameters(), argumentClass);

        WorkflowInstance workflowInstance = job.getJobNodeInstance().getWorkflowInstance();
        Map<String, Object> runtimeProperties = workflowInstance.getRuntimeProperties();

        JobContext.create(job.getId(), job.getJobDefinition().getJobType(), runtimeProperties);

        jobHandler.onUpdateJob(jobParameters);

        workflowInstance.addRuntimeProperties(JobContext.current().getRuntimeVariables());
    }


    @Override
    public Map<String, Object> createScheduledJobParameters(String jobType) {
        var jobHandler = JobHandlerRegistry.getJobHandler(jobType);
        return jobHandler.getScheduler().createScheduledJobParameters();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> collectSummaryData(Job job) {
        var jobHandler = (JobHandler<JobParameters>) JobHandlerRegistry.getJobHandler(
                job.getJobDefinition().getJobType()
        );
        Map<String, Object> runtimeProperties = job.getJobNodeInstance().getWorkflowInstance().getRuntimeProperties();
        JobContext.create(job.getId(), job.getJobDefinition().getJobType(), runtimeProperties);
        return jobHandler.collectSummaryData(jobHandler.toTypedJobParameters(job.getParameters()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void preCreateJob(Job job) {
        var jobHandler = (JobHandler<JobParameters>) JobHandlerRegistry.getJobHandler(
                job.getJobDefinition().getJobType()
        );

        Class<?> argumentClass = jobHandler.getParameterClass();
        JobParameters jobParameters = (JobParameters) JSON.convert(job.getParameters(), argumentClass);

        WorkflowInstance workflowInstance = job.getJobNodeInstance().getWorkflowInstance();
        Map<String, Object> runtimeProperties = workflowInstance.getRuntimeProperties();

        JobContext.create(job.getId(), job.getJobDefinition().getJobType(), runtimeProperties);

        jobHandler.preCreateJob(jobParameters);

        workflowInstance.addRuntimeProperties(JobContext.current().getRuntimeVariables());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> prepareRuntimeProperties(String jobType, Map<String, Object> parameters) {
        var jobHandler = (JobHandler<JobParameters>) JobHandlerRegistry.getJobHandler(
                jobType
        );

        Class<?> argumentClass = jobHandler.getParameterClass();
        JobParameters jobParameters = (JobParameters) JSON.convert(parameters, argumentClass);

        try {
            return jobHandler.prepareRuntimeProperties(jobParameters);
        }catch (Exception e){
            log.warn("Failed to prepare runtime properties for job.", e);
            return Map.of();
        }
    }
}
