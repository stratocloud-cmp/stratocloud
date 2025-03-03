package com.stratocloud.resource.jobs;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.job.AutoRegisteredJobHandler;
import com.stratocloud.job.JobScheduler;
import com.stratocloud.messaging.MessageBus;
import com.stratocloud.resource.ResourceService;
import com.stratocloud.resource.cmd.SynchronizeResourceStatesCmd;
import org.springframework.stereotype.Component;

@Component
public class SynchronizeResourceStatesJobHandler
        implements AutoRegisteredJobHandler<SynchronizeResourceStatesCmd> {

    private final ResourceService resourceService;

    private final SynchronizeResourceStatesJobScheduler scheduler;

    private final MessageBus messageBus;

    public SynchronizeResourceStatesJobHandler(ResourceService resourceService,
                                               SynchronizeResourceStatesJobScheduler scheduler,
                                               MessageBus messageBus) {
        this.resourceService = resourceService;
        this.scheduler = scheduler;
        this.messageBus = messageBus;
    }

    @Override
    public String getJobType() {
        return "SYNCHRONIZE_RESOURCE_STATES";
    }

    @Override
    public String getJobTypeName() {
        return "同步云资源状态";
    }

    @Override
    public String getStartJobTopic() {
        return "SYNCHRONIZE_RESOURCE_STATES_JOB_START";
    }

    @Override
    public String getCancelJobTopic() {
        return "SYNCHRONIZE_RESOURCE_STATES_JOB_CANCEL";
    }

    @Override
    public String getServiceName() {
        return StratoServices.RESOURCE_SERVICE;
    }

    @Override
    public boolean defaultWorkflowRequireOrder() {
        return false;
    }

    @Override
    public void preCreateJob(SynchronizeResourceStatesCmd parameters) {

    }

    @Override
    public void onUpdateJob(SynchronizeResourceStatesCmd parameters) {

    }

    @Override
    public void onCancelJob(String message, SynchronizeResourceStatesCmd parameters) {

    }

    @Override
    public void onStartJob(SynchronizeResourceStatesCmd parameters) {
        tryFinishJob(messageBus, () -> resourceService.synchronizeResourceStates(parameters));
    }


    @Override
    public JobScheduler getScheduler() {
        return scheduler;
    }


}
