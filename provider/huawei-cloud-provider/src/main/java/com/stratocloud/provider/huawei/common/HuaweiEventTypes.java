package com.stratocloud.provider.huawei.common;

import com.stratocloud.event.StratoEventType;
import com.stratocloud.provider.ResourceEventTypes;

import java.util.Optional;
import java.util.Set;

public class HuaweiEventTypes {
    public record HuaweiEventType(StratoEventType eventType, String externalEventName){}

    public static final HuaweiEventType INSTANCE_STARTED = new HuaweiEventType(
            ResourceEventTypes.INSTANCE_STARTED_EVENT, "startServer"
    );

    public static final HuaweiEventType INSTANCE_STOPPED = new HuaweiEventType(
            ResourceEventTypes.INSTANCE_STOPPED_EVENT, "stopServer"
    );

    public static final HuaweiEventType INSTANCE_CREATED = new HuaweiEventType(
            ResourceEventTypes.INSTANCE_CREATED_EVENT, "createServer"
    );


    public static final HuaweiEventType INSTANCE_DESTROYED = new HuaweiEventType(
            ResourceEventTypes.INSTANCE_DESTROYED_EVENT, "deleteServer"
    );

    public static final HuaweiEventType INSTANCE_REBOOTED = new HuaweiEventType(
            ResourceEventTypes.INSTANCE_RESTART_EVENT, "rebootServer"
    );

    public static final Set<HuaweiEventType> instanceEventTypes = Set.of(
            INSTANCE_STARTED,
            INSTANCE_STOPPED,
            INSTANCE_CREATED,
            INSTANCE_DESTROYED,
            INSTANCE_REBOOTED
    );

    public static Optional<HuaweiEventType> fromInstanceEventName(String eventName){
        return instanceEventTypes.stream().filter(
                e -> e.externalEventName().equals(eventName)
        ).findAny();
    }
}
