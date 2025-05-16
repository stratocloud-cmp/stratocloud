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
}
