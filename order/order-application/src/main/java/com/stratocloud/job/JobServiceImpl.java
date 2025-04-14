package com.stratocloud.job;
import com.stratocloud.audit.AuditLogContext;
import com.stratocloud.audit.AuditObject;
import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.job.cmd.*;
import com.stratocloud.job.query.*;
import com.stratocloud.job.response.*;
import com.stratocloud.validate.ValidateRequest;
import org.springframework.data.domain.Pageable;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.external.order.JobHandlerGatewayService;
import com.stratocloud.identifier.SnowflakeId;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.order.Order;
import com.stratocloud.order.OrderFactory;
import com.stratocloud.repository.*;
import com.stratocloud.workflow.Workflow;
import com.stratocloud.workflow.nodes.JobNode;
import com.stratocloud.workflow.runtime.JobNodeInstance;
import com.stratocloud.workflow.runtime.WorkflowInstance;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JobServiceImpl implements JobService {

    private final JobRepository repository;
    private final JobDefinitionRepository definitionRepository;

    private final OrderFactory orderFactory;

    private final OrderRepository orderRepository;

    private final WorkflowRepository workflowRepository;

    private final WorkflowInstanceRepository workflowInstanceRepository;

    private final JobHandlerGatewayService jobHandlerGatewayService;

    private final ScheduledTriggerRepository triggerRepository;

    public JobServiceImpl(JobRepository repository,
                          JobDefinitionRepository definitionRepository,
                          OrderFactory orderFactory,
                          OrderRepository orderRepository,
                          WorkflowRepository workflowRepository,
                          WorkflowInstanceRepository workflowInstanceRepository,
                          JobHandlerGatewayService jobHandlerGatewayService,
                          ScheduledTriggerRepository triggerRepository) {
        this.repository = repository;
        this.definitionRepository = definitionRepository;
        this.orderFactory = orderFactory;
        this.orderRepository = orderRepository;
        this.workflowRepository = workflowRepository;
        this.workflowInstanceRepository = workflowInstanceRepository;
        this.jobHandlerGatewayService = jobHandlerGatewayService;
        this.triggerRepository = triggerRepository;
    }

    @Override
    @Transactional
    @ValidateRequest
    public CreateJobResponse createJob(CreateJobCmd cmd) {
        Long jobId = cmd.getJobId();
        String jobType = cmd.getJobType();
        Map<String, Object> jobParameters = cmd.getJobParameters();
        Map<String, Object> runtimeProperties = cmd.getRuntimeProperties();
        String note = cmd.getNote();

        boolean preCreateJobExecuted = true;

        if(jobId == null){
            preCreateJobExecuted = false;
            jobId = SnowflakeId.nextId();
        }

        JobDefinition jobDefinition = definitionRepository.findByJobType(jobType);
        Workflow defaultWorkflow = jobDefinition.getDefaultWorkflow().createReplica();

        if(!AuditLogContext.exists())
            AuditLogContext.current().setSpecificAction(
                    jobDefinition.getJobType(), jobDefinition.getJobTypeName()
            );

        defaultWorkflow = workflowRepository.saveWithSystemSession(defaultWorkflow);

        String workflowName = defaultWorkflow.getName();

        List<JobNode> jobNodes = defaultWorkflow.getNodesByType(JobNode.class);

        JobNode jobNode = jobNodes.stream().filter(
                n -> n.getJobDefinition().getJobType().equals(jobType)
        ).findAny().orElseThrow(
                () -> new StratoException("Invalid default workflow: %s.".formatted(workflowName))
        );


        if(runtimeProperties == null)
            runtimeProperties = new HashMap<>();
        else
            runtimeProperties = new HashMap<>(runtimeProperties);

        JobContext.mergeRuntimeProperties(
                runtimeProperties,
                jobHandlerGatewayService.prepareRuntimeProperties(jobType, jobParameters)
        );

        if(jobDefinition.getDefaultWorkflowRequireOrder()){
            Order order = orderFactory.createOrder(note, defaultWorkflow, jobNode.getNodeKey(), jobParameters);

            order.createWorkflowInstance(runtimeProperties);

            Job job = order.getOrderItems().get(0).getJobNodeInstance().getJob();
            job.reassignId(jobId);

            if(!preCreateJobExecuted){
                jobHandlerGatewayService.preCreateJob(job);
            }

            order.onSubmit(note, runtimeProperties);
            order.collectSummaryData();
            orderRepository.save(order);
        }else {
            WorkflowInstance workflowInstance = defaultWorkflow.createInstance(runtimeProperties);
            JobNodeInstance nodeInstance = (JobNodeInstance) workflowInstance.getNodeInstanceByNodeId(jobNode.getId());
            Job job = nodeInstance.getJob();
            job.setParameters(jobParameters);
            job.reassignId(jobId);

            if(!preCreateJobExecuted){
                jobHandlerGatewayService.preCreateJob(job);
            }

            workflowInstance.start();
            workflowInstanceRepository.save(workflowInstance);
        }



        return new CreateJobResponse(jobId);
    }


    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedJobResponse> describeJobs(DescribeJobsRequest request) {
        JobFilters jobFilters = new JobFilters(
                request.getJobIds(),
                request.getTenantIds(),
                request.getOwnerIds(),
                request.getJobStatuses(),
                request.getSearch()
        );
        Page<Job> page = repository.page(jobFilters, request.getPageable());

        return page.map(this::toNestedJobResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedJobDefinitionResponse> describeJobDefinitions(DescribeJobDefinitionsRequest request) {
        List<String> jobTypes = request.getJobTypes();
        String search = request.getSearch();
        Pageable pageable = request.getPageable();

        Page<JobDefinition> page = definitionRepository.page(jobTypes, search, pageable);

        return page.map(this::toNestedJobDefinition);
    }

    @Override
    @Transactional
    @ValidateRequest
    public ChangeOrderRequirementResponse changeJobDefinitionOrderRequirement(ChangeOrderRequirementCmd cmd) {
        JobDefinition jobDefinition = definitionRepository.findByJobType(cmd.getJobType());

        AuditLogContext.current().addAuditObject(
                new AuditObject(jobDefinition.getJobType(), jobDefinition.getJobTypeName())
        );

        jobDefinition.setDefaultWorkflowRequireOrder(cmd.getDefaultWorkflowRequireOrder());

        definitionRepository.save(jobDefinition);

        return new ChangeOrderRequirementResponse();
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedJobTriggerResponse> describeJobTriggers(DescribeJobTriggersRequest request) {
        String search = request.getSearch();
        Pageable pageable = request.getPageable();

        Page<ScheduledTrigger> page = triggerRepository.page(search, pageable);

        return page.map(this::toNestedJobTriggerResponse);
    }

    private NestedJobTriggerResponse toNestedJobTriggerResponse(ScheduledTrigger scheduledTrigger) {
        NestedJobTriggerResponse nestedJobTriggerResponse = new NestedJobTriggerResponse();
        nestedJobTriggerResponse.setTriggerId(scheduledTrigger.getTriggerId());
        nestedJobTriggerResponse.setJobType(scheduledTrigger.getJobDefinition().getJobType());
        nestedJobTriggerResponse.setJobTypeName(scheduledTrigger.getJobDefinition().getJobTypeName());
        nestedJobTriggerResponse.setCronExpression(scheduledTrigger.getCronExpression());
        nestedJobTriggerResponse.setNextTriggerTime(scheduledTrigger.getNextTriggerTime());
        nestedJobTriggerResponse.setDisabled(scheduledTrigger.getDisabled());
        nestedJobTriggerResponse.setDescription(scheduledTrigger.getDescription());
        return nestedJobTriggerResponse;
    }


    @Override
    @Transactional
    @ValidateRequest
    public UpdateJobTriggerResponse updateJobTrigger(UpdateJobTriggerCmd cmd) {
        String triggerId = cmd.getTriggerId();
        String cronExpression = cmd.getCronExpression();
        String description = cmd.getDescription();

        ScheduledTrigger trigger = triggerRepository.findTrigger(triggerId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(triggerId, triggerId)
        );

        trigger.update(cronExpression, description);

        triggerRepository.save(trigger);

        return new UpdateJobTriggerResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public EnableJobTriggerResponse enableTrigger(EnableJobTriggerCmd cmd) {
        String triggerId = cmd.getTriggerId();

        ScheduledTrigger trigger = triggerRepository.findTrigger(triggerId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(triggerId, triggerId)
        );

        trigger.enable();

        triggerRepository.save(trigger);

        return new EnableJobTriggerResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public DisableJobTriggerResponse disableTrigger(DisableJobTriggerCmd cmd) {
        String triggerId = cmd.getTriggerId();

        ScheduledTrigger trigger = triggerRepository.findTrigger(triggerId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(triggerId, triggerId)
        );

        trigger.disable();

        triggerRepository.save(trigger);

        return new DisableJobTriggerResponse();
    }


    @Override
    public CreateJobCmd createCmdForScheduledJob(JobDefinition jobDefinition){
        String jobType = jobDefinition.getJobType();
        Map<String, Object> parameters = jobHandlerGatewayService.createScheduledJobParameters(jobType);

        final CreateJobCmd createJobCmd = new CreateJobCmd();
        createJobCmd.setJobType(jobType);
        createJobCmd.setJobParameters(parameters);
        createJobCmd.setNote("This is triggered automatically by the system.");
        createJobCmd.setRuntimeProperties(new HashMap<>());

        return createJobCmd;
    }

    @Override
    @Transactional
    @ValidateRequest
    public TriggerJobOnceResponse triggerJobOnce(TriggerJobOnceCmd cmd) {
        String triggerId = cmd.getTriggerId();

        ScheduledTrigger trigger = triggerRepository.findTrigger(triggerId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(triggerId, triggerId)
        );

        CreateJobCmd createJobCmd = createCmdForScheduledJob(trigger.getJobDefinition());

        createJob(createJobCmd);

        return new TriggerJobOnceResponse();
    }


    @Override
    @Transactional
    @ValidateRequest
    public RetryJobResponse retryJob(RetryJobCmd cmd) {
        Job job = repository.findById(cmd.getJobId()).orElseThrow(
                () -> new EntityNotFoundException("Job not found.")
        );

        AuditLogContext.current().addAuditObject(
                new AuditObject(job.getId().toString(), job.getJobDefinition().getJobTypeName())
        );

        job.retry();
        repository.save(job);
        return new RetryJobResponse();
    }

    private NestedJobDefinitionResponse toNestedJobDefinition(JobDefinition jobDefinition) {
        NestedJobDefinitionResponse response = new NestedJobDefinitionResponse();
        response.setJobType(jobDefinition.getJobType());
        response.setJobTypeName(jobDefinition.getJobTypeName());
        response.setStartJobTopic(jobDefinition.getStartJobTopic());
        response.setCancelJobTopic(jobDefinition.getCancelJobTopic());
        response.setServiceName(jobDefinition.getServiceName());
        if(jobDefinition.getDefaultWorkflow()!=null){
            response.setDefaultWorkflowId(jobDefinition.getDefaultWorkflow().getId());
            response.setDefaultWorkflowName(jobDefinition.getDefaultWorkflow().getName());
        }
        response.setDefaultWorkflowRequireOrder(jobDefinition.getDefaultWorkflowRequireOrder());
        return response;
    }

    private NestedJobResponse toNestedJobResponse(Job job) {
        NestedJobResponse response = new NestedJobResponse();

        EntityUtil.copyBasicFields(job, response);

        response.setManualStart(job.getManualStart());
        response.setPlannedStartTime(job.getPlannedStartTime());
        response.setStatus(job.getStatus());
        response.setParameters(job.getParameters());
        response.setStartedAt(job.getStartedAt());
        response.setEndedAt(job.getEndedAt());
        response.setErrorMessage(job.getErrorMessage());
        response.setJobType(job.getJobDefinition().getJobType());
        response.setJobTypeName(job.getJobDefinition().getJobTypeName());

        return response;
    }
}
