package com.stratocloud.order.cmd;

import lombok.Data;

import java.util.Map;

@Data
public class NestedOrderItemCmd {
    private String jobNodeKey;
    private Map<String, Object> parameters;
}
