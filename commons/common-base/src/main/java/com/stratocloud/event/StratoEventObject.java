package com.stratocloud.event;

import java.util.List;

public record StratoEventObject(String objectType,
                                String objectTypeName,
                                Long objectId,
                                String objectName,
                                Long ownerId,
                                List<Long> orderHandlerIds) {
}
