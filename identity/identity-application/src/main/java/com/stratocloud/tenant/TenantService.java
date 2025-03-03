package com.stratocloud.tenant;

import com.stratocloud.tenant.cmd.*;
import com.stratocloud.tenant.query.*;
import com.stratocloud.tenant.response.*;
import org.springframework.data.domain.Page;

public interface TenantService {
    CreateTenantResponse createTenant(CreateTenantCmd cmd);

    UpdateTenantResponse updateTenant(UpdateTenantCmd cmd);

    DisableTenantsResponse disableTenants(DisableTenantsCmd cmd);

    DeleteTenantsResponse deleteTenants(DeleteTenantsCmd cmd);

    DescribeSimpleTenantsResponse describeInheritedTenants(DescribeInheritedTenantsRequest request);

    DescribeSimpleTenantsResponse describeSubTenants(DescribeSubTenantsRequest request);

    Page<NestedTenantResponse> describeTenants(DescribeTenantsRequest request);

    DescribeTenantsTreeResponse describeTenantsTree(DescribeTenantsTreeRequest request);

    EnableTenantsResponse enableTenants(EnableTenantsCmd cmd);
}
