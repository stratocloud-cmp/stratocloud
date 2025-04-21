package com.stratocloud.job;

import com.stratocloud.auth.RunWithSystemSession;
import com.stratocloud.external.resource.OrderJobGatewayService;
import com.stratocloud.identifier.SnowflakeId;
import com.stratocloud.job.cmd.RunAsyncJobCmd;
import com.stratocloud.job.query.DescribeAsyncJobsRequest;
import com.stratocloud.job.query.NestedAsyncJobResponse;
import com.stratocloud.job.response.RunAsyncJobResponse;
import com.stratocloud.lock.DistributedLock;
import com.stratocloud.repository.AsyncJobRepository;
import com.stratocloud.repository.TaskRepository;
import com.stratocloud.request.JobParameters;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.stratocloud.utils.concurrent.ConcurrentUtil;
import com.stratocloud.validate.ValidateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class AsyncJobServiceImpl implements AsyncJobService {

    private final AsyncJobRepository repository;

    private final OrderJobGatewayService orderJobGatewayService;

    private final TaskRepository taskRepository;

    private final AsyncJobAssembler assembler;


    public AsyncJobServiceImpl(AsyncJobRepository repository,
                               OrderJobGatewayService orderJobGatewayService,
                               TaskRepository taskRepository,
                               AsyncJobAssembler assembler) {
        this.repository = repository;
        this.orderJobGatewayService = orderJobGatewayService;
        this.taskRepository = taskRepository;
        this.assembler = assembler;
    }

    @Override
    @Transactional
    @ValidateRequest
    public RunAsyncJobResponse runAsyncJob(RunAsyncJobCmd cmd) {
        String jobType = cmd.getJobType();
        JobHandler<JobParameters> jobHandler = getJobHandler(jobType);
        JobParameters jobParameters = getJobParameters(cmd, jobHandler);

        JobContext jobContext = createJobContext(jobType);

        jobHandler.preCreateJob(jobParameters);

        orderJobGatewayService.createSingleJob(
                jobContext.getJobId(), jobType, jobParameters, cmd.getNote(), jobContext.getRuntimeVariables()
        );

        return new RunAsyncJobResponse(jobContext.getJobId());
    }


    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedAsyncJobResponse> describeAsyncJobs(DescribeAsyncJobsRequest request) {
        List<Long> jobIds = request.getJobIds();
        Pageable pageable = request.getPageable();

        Page<AsyncJob> page = repository.page(jobIds, pageable);

        return page.map(assembler::toNestedAsyncJobResponse);
    }

    private static JobParameters getJobParameters(RunAsyncJobCmd cmd, JobHandler<?> jobHandler) {
        Class<?> argumentClass = jobHandler.getParameterClass();
        return (JobParameters) JSON.convert(cmd.getJobParameters(), argumentClass);
    }

    private static JobContext createJobContext(String jobType) {
        Long jobId = SnowflakeId.nextId();
        JobContext.create(jobId, jobType);
        return JobContext.current();
    }


    @SuppressWarnings("unchecked")
    private static JobHandler<JobParameters> getJobHandler(String jobType) {
        return (JobHandler<JobParameters>) JobHandlerRegistry.getJobHandler(jobType);
    }



    @Scheduled(fixedDelay = 10000L)
    @DistributedLock(lockName = "CHECK_TASK_RESULTS_AND_STATES", waitIfLocked = false)
    @RunWithSystemSession
    public void checkTasksResultsAndStates(){
        checkTasksResults();
        checkTasksStates();
    }

    private void checkTasksResults(){
        List<Task> startedTasks = taskRepository.findByState(TaskState.STARTED);

        if(Utils.isEmpty(startedTasks))
            return;

        List<Runnable> checkingTasks = startedTasks.stream().map(
                task -> (Runnable) task::checkResult
        ).toList();
        ConcurrentUtil.runAndWait(checkingTasks);

        List<Runnable> savingTasks = startedTasks.stream().map(
                task -> (Runnable) () -> taskRepository.saveWithoutTransaction(task)
        ).toList();

        ConcurrentUtil.runAndWait(savingTasks);
    }
    private void checkTasksStates() {
        List<AsyncJob> jobsNotEnded = repository.findByStartedAndEnded(true, false);

        if (Utils.isEmpty(jobsNotEnded))
            return;

        log.info("Checking {} jobs tasks states.", jobsNotEnded.size());

        for (AsyncJob asyncJob : jobsNotEnded) {
            try {
                AsyncJobServiceImpl currentProxy = (AsyncJobServiceImpl) AopContext.currentProxy();
                currentProxy.checkSingleJobTasksStates(asyncJob.getId());
            }catch (Exception e){
                log.error("Failed to check tasks states of async job {}. Retrying later.", asyncJob.getId(), e);
            }
        }
    }

    @Transactional
    public void checkSingleJobTasksStates(Long jobId) {
        AsyncJob lockedAsyncJob = repository.lockAsyncJob(jobId);
        lockedAsyncJob.checkTasksStates();
        lockedAsyncJob.checkIfNotStartedSuccessfully();

        AsyncJobHandler<?> asyncJobHandler = AsyncJobService.getAsyncJobHandler(lockedAsyncJob);

        if(lockedAsyncJob.getEnded()){
            asyncJobHandler.onJobEnded(lockedAsyncJob);
        }

        if(lockedAsyncJob.getEnded() && asyncJobHandler.isTransientJob()) {
            lockedAsyncJob.clearExecutions();
            log.info("Transient job {} is ended, and executions cleared.", lockedAsyncJob.getId());
        }


        repository.save(lockedAsyncJob);
    }


}
