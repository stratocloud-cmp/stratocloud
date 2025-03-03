package com.stratocloud.resource.query;

import lombok.Data;

@Data
public class NestedRuntimeProperty {
    private String key;
    private String keyName;
    private String value;
    private String valueName;
    private Boolean displayable;
    private Boolean searchable;
    private Boolean displayInList;
}
