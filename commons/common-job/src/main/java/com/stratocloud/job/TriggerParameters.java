package com.stratocloud.job;

public record TriggerParameters(boolean autoCreateTrigger,
                                String triggerId,
                                String triggerCron,
                                boolean disabled,
                                String description) {
}
