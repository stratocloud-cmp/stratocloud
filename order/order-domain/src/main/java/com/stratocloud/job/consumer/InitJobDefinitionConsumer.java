package com.stratocloud.job.consumer;

import com.stratocloud.job.InitJobDefinitionPayload;
import com.stratocloud.job.JobDefinition;
import com.stratocloud.job.ScheduledTrigger;
import com.stratocloud.job.TriggerParameters;
import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageConsumer;
import com.stratocloud.repository.JobDefinitionRepository;
import com.stratocloud.repository.ScheduledTriggerRepository;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InitJobDefinitionConsumer implements MessageConsumer {

    private final JobDefinitionRepository jobDefinitionRepository;

    private final ScheduledTriggerRepository triggerRepository;

    public InitJobDefinitionConsumer(JobDefinitionRepository jobDefinitionRepository,
                                     ScheduledTriggerRepository triggerRepository) {
        this.jobDefinitionRepository = jobDefinitionRepository;
        this.triggerRepository = triggerRepository;
    }

    @Override
    @Transactional
    public void consume(Message message) {
        InitJobDefinitionPayload payload = JSON.toJavaObject(message.getPayload(), InitJobDefinitionPayload.class);


        JobDefinition jobDefinition;

        if(jobDefinitionRepository.existsById(payload.jobType())) {
            jobDefinition = jobDefinitionRepository.findByJobType(payload.jobType());
            jobDefinition.setJobTypeName(payload.jobTypeName());
            jobDefinition.setDefaultWorkflowRequireOrder(payload.defaultWorkflowRequireOrder());
        }else {
            jobDefinition = new JobDefinition(
                    payload.jobType(),
                    payload.jobTypeName(),
                    payload.startJobTopic(),
                    payload.cancelJobTopic(),
                    payload.serviceName(),
                    payload.defaultWorkflowRequireOrder()
            );
        }

        jobDefinition = jobDefinitionRepository.save(jobDefinition);

        TriggerParameters triggerParameters = payload.triggerParameters();

        if(triggerParameters.autoCreateTrigger()){
            if(!triggerRepository.existsById(triggerParameters.triggerId())){
                ScheduledTrigger scheduledTrigger = new ScheduledTrigger(
                        jobDefinition,
                        triggerParameters.triggerId(),
                        triggerParameters.triggerCron(),
                        triggerParameters.description()
                );

                if(triggerParameters.disabled())
                    scheduledTrigger.disable();
                else
                    scheduledTrigger.enable();

                triggerRepository.save(scheduledTrigger);
            }
        }
    }

    @Override
    public String getTopic() {
        return "INIT_JOB_DEFINITION";
    }

    @Override
    public String getConsumerGroup() {
        return "JOB";
    }
}
