package com.stratocloud.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

@Builder(toBuilder = true)
public record ExternalResource(String providerId,
                               Long accountId,
                               String category,
                               String type,
                               String externalId,
                               String name,
                               ResourceState state) {

    @JsonIgnore
    public boolean isStoppedOrShutdown() {
        return state == ResourceState.STOPPED || state == ResourceState.SHUTDOWN;
    }

    @JsonIgnore
    public boolean isStarted() {
        return state == ResourceState.STARTED;
    }

    @JsonIgnore
    public boolean isInUse() {
        return state == ResourceState.IN_USE;
    }

    @JsonIgnore
    public boolean isDetaching() {
        return state == ResourceState.DETACHING;
    }

    @JsonIgnore
    public boolean isAttaching() {
        return state == ResourceState.ATTACHING;
    }
}
