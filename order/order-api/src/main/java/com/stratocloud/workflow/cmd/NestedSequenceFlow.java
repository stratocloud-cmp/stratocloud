package com.stratocloud.workflow.cmd;

import lombok.Data;

@Data
public class NestedSequenceFlow {
    private String sourceNodeKey;
    private String targetNodeKey;
}
