package com.stratocloud.event;

public record StratoEventObject(String objectType,
                                String objectTypeName,
                                Long objectId,
                                String objectName,
                                Long ownerId) {
}
