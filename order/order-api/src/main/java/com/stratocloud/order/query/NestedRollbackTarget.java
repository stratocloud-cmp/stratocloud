package com.stratocloud.order.query;

import com.stratocloud.identity.SimpleUser;
import lombok.Data;

import java.util.List;

@Data
public class NestedRollbackTarget {
    private Long nodeId;
    private String nodeName;
    private Long nodeInstanceId;

    private List<SimpleUser> possibleHandlers;
}
