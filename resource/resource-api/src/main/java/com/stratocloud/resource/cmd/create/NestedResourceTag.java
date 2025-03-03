package com.stratocloud.resource.cmd.create;

import lombok.Data;

@Data
public class NestedResourceTag {
    private String tagKey;
    private String tagKeyName;
    private String tagValue;
    private String tagValueName;
}
