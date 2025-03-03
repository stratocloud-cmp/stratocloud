package com.stratocloud.provider.relationship;

import com.stratocloud.resource.Relationship;

public interface EssentialRequirementHandler extends ExclusiveRequirementHandler{
    @Override
    default void connect(Relationship relationship) {

    }

    @Override
    default void disconnect(Relationship relationship) {

    }

    @Override
    default boolean disconnectOnLost() {
        return true;
    }
}
