package com.stratocloud.resource;

import java.util.Map;

public record ExternalRequirement(String relationshipTypeId, ExternalResource target, Map<String, Object> properties) {
}
