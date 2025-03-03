package com.stratocloud.job;

public record InitJobDefinitionPayload(String jobType,
                                       String jobTypeName,
                                       String startJobTopic,
                                       String cancelJobTopic,
                                       String serviceName,
                                       TriggerParameters triggerParameters,
                                       boolean defaultWorkflowRequireOrder) {
}
