package com.stratocloud.workflow.runtime;

import com.stratocloud.identity.SimpleUser;

import java.util.List;

public interface RollbackTarget {
    Long getNodeId();
    String getNodeName();
    Long getNodeInstanceId();

    List<SimpleUser> getPossibleHandlers();
}
