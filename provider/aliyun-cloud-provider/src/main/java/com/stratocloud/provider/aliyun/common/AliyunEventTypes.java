package com.stratocloud.provider.aliyun.common;

import com.stratocloud.event.StratoEventType;
import com.stratocloud.provider.ResourceEventTypes;

import java.util.Optional;
import java.util.Set;

public class AliyunEventTypes {
    public record AliyunEventType(StratoEventType eventType, String externalEventName){}

    public static final AliyunEventType INSTANCE_STARTED = new AliyunEventType(
            ResourceEventTypes.INSTANCE_STARTED_EVENT, "StartInstance"
    );

    public static final AliyunEventType INSTANCE_STOPPED = new AliyunEventType(
            ResourceEventTypes.INSTANCE_STOPPED_EVENT, "StopInstance"
    );

    public static final AliyunEventType INSTANCE_CREATED = new AliyunEventType(
            ResourceEventTypes.INSTANCE_CREATED_EVENT, "RunInstances"
    );

    public static final AliyunEventType INSTANCE_DESTROYED = new AliyunEventType(
            ResourceEventTypes.INSTANCE_DESTROYED_EVENT, "DeleteInstance"
    );

    public static final AliyunEventType INSTANCE_REBOOTED = new AliyunEventType(
            ResourceEventTypes.INSTANCE_RESTART_EVENT, "RebootInstance"
    );

    public static final Set<AliyunEventType> instanceEventTypes = Set.of(
            INSTANCE_STARTED,
            INSTANCE_STOPPED,
            INSTANCE_CREATED,
            INSTANCE_DESTROYED,
            INSTANCE_REBOOTED
    );

    public static Optional<AliyunEventType> fromInstanceEventName(String eventName){
        return instanceEventTypes.stream().filter(
                e -> e.externalEventName().equals(eventName)
        ).findAny();
    }
}
