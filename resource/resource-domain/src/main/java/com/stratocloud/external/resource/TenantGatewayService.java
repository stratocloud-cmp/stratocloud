package com.stratocloud.external.resource;

import com.stratocloud.identity.SimpleTenant;

import java.util.List;

public interface TenantGatewayService {
    List<SimpleTenant> findInheritedTenants(Long tenantId);

    List<SimpleTenant> findSubTenants(Long tenantId);
}
