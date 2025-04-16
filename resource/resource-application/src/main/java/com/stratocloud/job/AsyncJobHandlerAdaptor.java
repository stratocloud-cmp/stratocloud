package com.stratocloud.job;

import com.stratocloud.audit.AuditLogContext;
import com.stratocloud.audit.AuditObject;
import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.repository.AsyncJobRepository;
import com.stratocloud.request.JobParameters;
import com.stratocloud.utils.ContextUtil;
import com.stratocloud.utils.Utils;
import com.stratocloud.utils.concurrent.SleepUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
public class AsyncJobHandlerAdaptor<P extends JobParameters> implements AsyncJobHandler<P> {

    private final AsyncJobHandler<P> jobHandler;

    public AsyncJobHandlerAdaptor(AsyncJobHandler<P> jobHandler) {
        this.jobHandler = jobHandler;
    }

    @Override
    public String getJobType() {
        return jobHandler.getJobType();
    }

    @Override
    public String getJobTypeName() {
        return jobHandler.getJobTypeName();
    }

    @Override
    public String getStartJobTopic() {
        return jobHandler.getStartJobTopic();
    }

    @Override
    public String getCancelJobTopic() {
        return jobHandler.getCancelJobTopic();
    }

    @Override
    public String getServiceName() {
        return jobHandler.getServiceName();
    }

    private AsyncJobRepository getAsyncJobRepository(){
        return ContextUtil.getBean(AsyncJobRepository.class);
    }

    @Override
    @Transactional
    public void preCreateJob(P parameters) {
        AsyncJobContext.create();
        preResolveAuditLog();
        jobHandler.preCreateJob(parameters);
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        getAsyncJobRepository().save(asyncJob);

        postResolveAuditLog(asyncJob);

        AsyncJobContext.destroy();
    }

    private void preResolveAuditLog() {
        if(AuditLogContext.exists())
            return;

        AuditLogContext auditLogContext = AuditLogContext.current();
        auditLogContext.setSpecificAction(
                jobHandler.getJobType(),
                jobHandler.getJobTypeName()
        );
    }

    private void postResolveAuditLog(AsyncJob asyncJob) {
        if(AuditLogContext.exists() && Utils.isNotEmpty(AuditLogContext.current().getAuditObjects()))
            return;

        AuditLogContext auditLogContext = AuditLogContext.current();
        auditLogContext.setSpecificAction(
                jobHandler.getJobType(),
                jobHandler.getJobTypeName()
        );
        if(Utils.isNotEmpty(asyncJob.getTasks())){
            List<Task> resourceTasks = asyncJob.getTasks().stream().filter(
                    t -> "Resource".equals(t.getEntityClass().getSimpleName())
            ).toList();

            if(Utils.isNotEmpty(resourceTasks)){
                auditLogContext.setSpecificObjectType("Resource", "资源");
                for (Task resourceTask : resourceTasks) {
                    auditLogContext.addAuditObject(
                            new AuditObject(resourceTask.getEntityId().toString(), resourceTask.getEntityDescription())
                    );
                }
            } else {
                Task firstTask = asyncJob.getTasks().get(0);
                String objectType = firstTask.getEntityClass().getSimpleName();
                auditLogContext.setSpecificObjectType(objectType, objectType);

                for (Task task : asyncJob.getTasks()) {
                    auditLogContext.addAuditObject(
                            new AuditObject(task.getEntityId().toString(), task.getEntityDescription())
                    );
                }
            }
        }
    }

    @Override
    @Transactional
    public void onUpdateJob(P parameters) {
        AsyncJob asyncJob = getAsyncJobRepository().lockAsyncJob(JobContext.current().getJobId());
        AsyncJobContext.create(asyncJob);
        jobHandler.onUpdateJob(parameters);
        getAsyncJobRepository().saveWithSystemSession(asyncJob);
        AsyncJobContext.destroy();
    }

    @Override
    @Transactional
    public void onCancelJob(String message, P parameters) {
        AsyncJob asyncJob = getAsyncJobRepository().lockAsyncJob(JobContext.current().getJobId());
        AsyncJobContext.create(asyncJob);
        jobHandler.onCancelJob(message, parameters);
        getAsyncJobRepository().saveWithSystemSession(asyncJob);
        AsyncJobContext.destroy();

    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void onStartJob(P parameters) {
        Long jobId = JobContext.current().getJobId();
        AsyncJob asyncJob;
        try {
            asyncJob = getAsyncJobRepository().lockAsyncJob(jobId);
        }catch (EntityNotFoundException e){
            int retryAfterSeconds = 3;
            log.warn("Attempting to start async job {} but it's not found, retrying {}s later",
                    jobId, retryAfterSeconds);
            SleepUtil.sleep(retryAfterSeconds);
            asyncJob = getAsyncJobRepository().lockAsyncJob(jobId);
        }

        AsyncJobContext.create(asyncJob);
        jobHandler.onStartJob(parameters);
        getAsyncJobRepository().saveWithSystemSession(asyncJob);
        AsyncJobContext.destroy();
    }

    @Override
    @Transactional
    public void onJobEnded(AsyncJob asyncJob) {
        AsyncJobContext.create(asyncJob);
        jobHandler.onJobEnded(asyncJob);
        getAsyncJobRepository().save(asyncJob);
        AsyncJobContext.destroy();
    }


    @Override
    public boolean defaultWorkflowRequireOrder() {
        return jobHandler.defaultWorkflowRequireOrder();
    }

    @Override
    public JobScheduler getScheduler() {
        return jobHandler.getScheduler();
    }


    @Override
    public Class<P> getParameterClass() {
        return jobHandler.getParameterClass();
    }

    @Override
    public boolean isTransientJob() {
        return jobHandler.isTransientJob();
    }


    @Override
    @Transactional
    public List<String> collectSummaryData(P jobParameters) {
        if(JobContext.exists()){
            AsyncJob asyncJob = getAsyncJobRepository().findById(JobContext.current().getJobId()).orElseThrow(
                    () -> new EntityNotFoundException("AsyncJob not found.")
            );
            AsyncJobContext.create(asyncJob);
            List<String> result = jobHandler.collectSummaryData(jobParameters);
            AsyncJobContext.destroy();
            return result;
        } else {
            AsyncJob asyncJob = new AsyncJob();
            AsyncJobContext.create(asyncJob);
            jobHandler.preCreateJob(jobParameters);
            List<String> result = jobHandler.collectSummaryData(jobParameters);
            jobHandler.onCancelJob("", jobParameters);
            AsyncJobContext.destroy();
            return result;
        }
    }

    @Override
    public Map<String, Object> prepareRuntimeProperties(P jobParameters) {
        return jobHandler.prepareRuntimeProperties(jobParameters);
    }
}
