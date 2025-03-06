package com.stratocloud.job;

import com.stratocloud.request.JobParameters;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageBus;
import com.stratocloud.messaging.MessageConsumer;
import com.stratocloud.utils.ContextUtil;
import com.stratocloud.utils.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class JobHandlerRegistry {
    private static final Map<String, JobHandler<?>> jobHandlerMap = new ConcurrentHashMap<>();

    public static void register(JobHandler<?> jobHandler, ApplicationContext applicationContext){
        String jobType = jobHandler.getJobType();

        if(jobHandlerMap.containsKey(jobType))
            throw new StratoException("Cannot register 2 job handlers with same job type: %s.".formatted(jobType));

        doRegister(jobHandler, applicationContext);
        log.info("Job handler {} registered.", jobHandler.getJobType());
    }

    public static JobHandler<?> getJobHandler(String jobType){
        JobHandler<?> jobHandler = jobHandlerMap.get(jobType);

        if(jobHandler == null)
            throw new StratoException("Job handler not found by job type: %s.".formatted(jobType));

        return jobHandler;
    }


    private static void doRegister(JobHandler<?> jobHandler, ApplicationContext applicationContext) {
        MessageBus messageBus = applicationContext.getBean(MessageBus.class);

        MessageConsumer startJobConsumer = buildStartJobConsumer(jobHandler);
        MessageConsumer cancelJobConsumer = buildCancelJobConsumer(jobHandler);

        ContextUtil.ensureBean(applicationContext, startJobConsumer, startJobConsumer.getTopic());
        ContextUtil.ensureBean(applicationContext, cancelJobConsumer, cancelJobConsumer.getTopic());

        Message initJobDefinitionMessage = buildInitJobDefinitionMessage(jobHandler);
        messageBus.publishWithSystemSession(initJobDefinitionMessage);

        jobHandlerMap.put(jobHandler.getJobType(), jobHandler);
    }

    private static Message buildInitJobDefinitionMessage(JobHandler<?> jobHandler) {
        return Message.create(
                "INIT_JOB_DEFINITION",
                new InitJobDefinitionPayload(
                        jobHandler.getJobType(),
                        jobHandler.getJobTypeName(),
                        jobHandler.getStartJobTopic(),
                        jobHandler.getCancelJobTopic(),
                        jobHandler.getServiceName(),
                        jobHandler.getScheduler().getTriggerParameters(),
                        jobHandler.defaultWorkflowRequireOrder()
                )
        );
    }

    private static MessageConsumer buildStartJobConsumer(JobHandler<?> jobHandler) {
        return new MessageConsumer() {
            @Override
            @SuppressWarnings("unchecked")
            public void consume(Message message) {
                StartJobPayload payload = JSON.toJavaObject(message.getPayload(), StartJobPayload.class);

                JobParameters jobParameters = JSON.convert(
                        payload.parameters(),
                        jobHandler.getParameterClass()
                );

                JobHandler<JobParameters> jh = (JobHandler<JobParameters>) jobHandler;

                JobContext.create(
                        payload.jobId(),
                        jobHandler.getJobType(),
                        payload.runtimeVariables()
                );

                jh.onStartJob(jobParameters);
            }

            @Override
            public String getTopic() {
                return jobHandler.getStartJobTopic();
            }

            @Override
            public String getConsumerGroup() {
                return "JOB_HANDLER";
            }
        };
    }

    private static MessageConsumer buildCancelJobConsumer(JobHandler<?> jobHandler) {
        return new MessageConsumer() {
            @Override
            @SuppressWarnings("unchecked")
            public void consume(Message message) {
                JobCanceledPayload payload = JSON.toJavaObject(message.getPayload(), JobCanceledPayload.class);

                JobParameters jobParameters = JSON.convert(
                        payload.parameters(),
                        jobHandler.getParameterClass()
                );

                JobHandler<JobParameters> jh = (JobHandler<JobParameters>) jobHandler;

                JobContext.create(payload.jobId(), jobHandler.getJobType());

                jh.onCancelJob(payload.message(), jobParameters);
            }

            @Override
            public String getTopic() {
                return jobHandler.getCancelJobTopic();
            }

            @Override
            public String getConsumerGroup() {
                return "JOB_HANDLER";
            }
        };
    }
}
