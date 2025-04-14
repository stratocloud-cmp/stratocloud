package com.stratocloud.job;

import com.stratocloud.request.JobParameters;
import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageBus;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public interface JobHandler<P extends JobParameters> {

    @SuppressWarnings("unchecked")
    default Class<P> getParameterClass(){
        return (Class<P>) Utils.getTypeArgumentClass(getClass(), JobHandler.class);
    }

    String getJobType();

    String getJobTypeName();

    String getStartJobTopic();

    String getCancelJobTopic();

    String getServiceName();

    void preCreateJob(P parameters);

    void onUpdateJob(P parameters);

    void onCancelJob(String message, P parameters);

    void onStartJob(P parameters);

    default List<String> collectSummaryData(P jobParameters){
        return new ArrayList<>();
    }

    default List<String> collectSummaryData(Map<String, Object> jobParametersMap){
        P jobParameters = toTypedJobParameters(jobParametersMap);
        return collectSummaryData(jobParameters);
    }

    default P toTypedJobParameters(Map<String, Object> jobParametersMap) {
        Class<P> argumentClass = getParameterClass();
        return JSON.convert(jobParametersMap, argumentClass);
    }

    default JobScheduler getScheduler(){
        return new DefaultJobScheduler();
    }

    default boolean defaultWorkflowRequireOrder(){
        return true;
    }




    default void tryFinishJob(MessageBus messageBus, Supplier<Object> callback){
        try {
            Object output = callback.get();
            Message message = Message.create(
                    JobTopics.WORKER_REPORT_JOB_FINISHED,
                    new WorkerReportJobFinishedPayload(JobContext.current().getJobId(), JSON.toMap(output))
            );
            messageBus.publish(message);
        }catch (Exception e){
            LoggerFactory.getLogger(JobHandler.class).error("Failed to finish job: ", e);
            Message message = Message.create(
                    JobTopics.WORKER_REPORT_JOB_FAILED,
                    new WorkerReportJobFailedPayload(JobContext.current().getJobId(), List.of(e.getMessage()))
            );
            messageBus.publish(message);
        }
    }

    default Map<String, Object> prepareRuntimeProperties(P jobParameters){
        return Map.of();
    }
}
