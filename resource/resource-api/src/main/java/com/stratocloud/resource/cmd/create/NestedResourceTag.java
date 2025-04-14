package com.stratocloud.resource.cmd.create;

import com.stratocloud.tag.NestedTag;
import lombok.Data;

@Data
public class NestedResourceTag implements NestedTag {
    private String tagKey;
    private String tagKeyName;
    private String tagValue;
    private String tagValueName;
}
