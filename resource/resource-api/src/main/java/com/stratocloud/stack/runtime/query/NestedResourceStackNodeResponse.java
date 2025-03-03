package com.stratocloud.stack.runtime.query;

import com.stratocloud.request.query.NestedAuditable;
import com.stratocloud.resource.query.NestedResourceResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NestedResourceStackNodeResponse extends NestedAuditable {
    private String nodeKey;
    private String nodeName;
    private NestedResourceResponse resource;
}
