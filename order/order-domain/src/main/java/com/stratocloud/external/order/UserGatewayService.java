package com.stratocloud.external.order;

import com.stratocloud.identity.SimpleUser;
import com.stratocloud.identity.SimpleUserCollector;

import java.util.List;

public interface UserGatewayService extends SimpleUserCollector {
    List<SimpleUser> findHandlersByRolesFromTenant(List<Long> roleIds);

    List<SimpleUser> findHandlersByRolesFromUserGroups(List<Long> roleIds);
}
