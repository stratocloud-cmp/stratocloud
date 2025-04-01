package com.stratocloud.resource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum ResourceState {
    NO_STATE,
    BUILD_ERROR,

    BUILDING,

    STARTED,
    STOPPING,
    STOPPED,
    STARTING,
    RESTARTING,

    CONFIGURING,

    SHUTDOWN,

    DESTROYING,
    DESTROYED,

    ERROR,

    UNKNOWN,

    AVAILABLE,
    UNAVAILABLE,
    SOLD_OUT,

    ATTACHING,
    DETACHING,

    IN_USE,
    IDLE,

    HEALTH_CHECK_NORMAL,
    HEALTH_CHECK_ABNORMAL,
    HEALTH_CHECK_UNAVAILABLE,

    REBUILDING,
    PAUSED,
    SUSPENDED,
    SHELVED,

    INSUFFICIENT_RESOURCE,
    ;



    public static List<ResourceState> getAliveStates(){
        List<ResourceState> states = new ArrayList<>(List.of(ResourceState.values()));
        List.of(NO_STATE, BUILD_ERROR, BUILDING, DESTROYED, DESTROYING).forEach(states::remove);
        return states;
    }

    public static Set<ResourceState> getAliveStateSet(){
        return new HashSet<>(getAliveStates());
    }

    public static List<ResourceState> getVisibleStates(){
        List<ResourceState> states = new ArrayList<>(List.of(ResourceState.values()));
        List.of(DESTROYED).forEach(states::remove);
        return states;
    }
}
