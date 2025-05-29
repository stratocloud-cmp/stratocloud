package com.stratocloud.provider;

import com.stratocloud.event.StratoEventType;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.ResourceActions;
import com.stratocloud.resource.event.ResourceActionsEventHandler;

public class ResourceEventTypes {
    public static final StratoEventType INSTANCE_STARTED_EVENT = ResourceActionsEventHandler.getEventType(
            ResourceCategories.COMPUTE_INSTANCE,
            ResourceActions.START,
            true
    );

    public static final StratoEventType INSTANCE_STOPPED_EVENT = ResourceActionsEventHandler.getEventType(
            ResourceCategories.COMPUTE_INSTANCE,
            ResourceActions.STOP,
            true
    );

    public static final StratoEventType INSTANCE_CREATED_EVENT = ResourceActionsEventHandler.getEventType(
            ResourceCategories.COMPUTE_INSTANCE,
            ResourceActions.BUILD_RESOURCE,
            true
    );

    public static final StratoEventType INSTANCE_DESTROYED_EVENT = ResourceActionsEventHandler.getEventType(
            ResourceCategories.COMPUTE_INSTANCE,
            ResourceActions.DESTROY_RESOURCE,
            true
    );

    public static final StratoEventType INSTANCE_RESTART_EVENT = ResourceActionsEventHandler.getEventType(
            ResourceCategories.COMPUTE_INSTANCE,
            ResourceActions.RESTART,
            true
    );




    public static final StratoEventType INSTANCE_HIGH_CPU_USAGE = new StratoEventType(
            "COMPUTE_INSTANCE.HIGH_CPU_USAGE",
            "云主机CPU利用率过高"
    );
    public static final StratoEventType INSTANCE_HIGH_MEMORY_USAGE = new StratoEventType(
            "COMPUTE_INSTANCE.HIGH_MEMORY_USAGE",
            "云主机内存利用率过高"
    );
    public static final StratoEventType INSTANCE_HIGH_DISK_USAGE = new StratoEventType(
            "COMPUTE_INSTANCE.HIGH_DISK_USAGE",
            "云主机磁盘利用率过高"
    );
    public static final StratoEventType INSTANCE_HIGH_BANDWIDTH_USAGE = new StratoEventType(
            "COMPUTE_INSTANCE.HIGH_BANDWIDTH_USAGE",
            "云主机网络带宽利用率过高"
    );

    public static final StratoEventType INSTANCE_HIGH_CPU_USAGE_RECOVERED = new StratoEventType(
            "COMPUTE_INSTANCE.HIGH_CPU_USAGE_RECOVERED",
            "云主机CPU利用率已恢复"
    );
    public static final StratoEventType INSTANCE_HIGH_MEMORY_USAGE_RECOVERED = new StratoEventType(
            "COMPUTE_INSTANCE.HIGH_MEMORY_USAGE_RECOVERED",
            "云主机内存利用率已恢复"
    );
    public static final StratoEventType INSTANCE_HIGH_DISK_USAGE_RECOVERED = new StratoEventType(
            "COMPUTE_INSTANCE.HIGH_DISK_USAGE_RECOVERED",
            "云主机磁盘利用率已恢复"
    );
    public static final StratoEventType INSTANCE_HIGH_BANDWIDTH_USAGE_RECOVERED = new StratoEventType(
            "COMPUTE_INSTANCE.HIGH_BANDWIDTH_USAGE_RECOVERED",
            "云主机网络带宽利用率已恢复"
    );

}
