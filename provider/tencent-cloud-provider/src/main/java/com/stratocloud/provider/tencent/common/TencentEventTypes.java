package com.stratocloud.provider.tencent.common;

import com.stratocloud.event.StratoEventType;
import com.stratocloud.provider.ResourceEventTypes;

import java.util.Optional;
import java.util.Set;

public class TencentEventTypes {
    public record TencentEventType(StratoEventType eventType,
                                   String externalEventName){}

    public static final TencentEventType INSTANCE_STARTED = new TencentEventType(
            ResourceEventTypes.INSTANCE_STARTED_EVENT,
            "StartInstances"
    );

    public static final TencentEventType INSTANCE_STOPPED = new TencentEventType(
            ResourceEventTypes.INSTANCE_STOPPED_EVENT,
            "StopInstances"
    );

    public static final TencentEventType INSTANCE_CREATED = new TencentEventType(
            ResourceEventTypes.INSTANCE_CREATED_EVENT,
            "RunInstances"
    );

    public static final TencentEventType INSTANCE_DESTROYED = new TencentEventType(
            ResourceEventTypes.INSTANCE_DESTROYED_EVENT,
            "TerminateInstances"
    );

    public static final TencentEventType INSTANCE_REBOOTED = new TencentEventType(
            ResourceEventTypes.INSTANCE_RESTART_EVENT,
            "RebootInstances"
    );

    public static final Set<TencentEventType> instanceEventTypes = Set.of(
            INSTANCE_STARTED,
            INSTANCE_STOPPED,
            INSTANCE_CREATED,
            INSTANCE_DESTROYED,
            INSTANCE_REBOOTED
    );

    public static Optional<TencentEventType> fromInstanceEventName(String eventName){
        return instanceEventTypes.stream().filter(
                e -> e.externalEventName().equals(eventName)
        ).findAny();
    }
}
