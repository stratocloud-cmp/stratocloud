package com.stratocloud.resource.cmd.create;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class NestedNewResource {
    private String resourceTypeId;

    private Long externalAccountId;

    private String resourceName;

    private Map<String, Object> properties;

    private List<NestedResourceTag> tags;

    private String description;
}
