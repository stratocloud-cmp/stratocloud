package com.stratocloud.workflow.cmd;

import lombok.Data;

import java.util.Map;

@Data
public class NestedWorkflowNode {
    private String nodeType;
    private String nodeKey;
    private String nodeName;
    private Map<String, Object> nodeProperties;
}
