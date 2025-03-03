package com.stratocloud.tenant;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.tenant.cmd.*;
import com.stratocloud.tenant.query.*;
import com.stratocloud.tenant.response.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface TenantApi {
    @PostMapping(path = StratoServices.IDENTITY_SERVICE + "/create-tenant")
    CreateTenantResponse createTenant(@RequestBody CreateTenantCmd cmd);
    @PostMapping(path = StratoServices.IDENTITY_SERVICE + "/update-tenant")
    UpdateTenantResponse updateTenant(@RequestBody UpdateTenantCmd cmd);
    @PostMapping(path = StratoServices.IDENTITY_SERVICE + "/disable-tenants")
    DisableTenantsResponse disableTenants(@RequestBody DisableTenantsCmd cmd);

    @PostMapping(path = StratoServices.IDENTITY_SERVICE + "/enable-tenants")
    EnableTenantsResponse enableTenants(@RequestBody EnableTenantsCmd cmd);

    @PostMapping(path = StratoServices.IDENTITY_SERVICE + "/delete-tenants")
    DeleteTenantsResponse deleteTenants(@RequestBody DeleteTenantsCmd cmd);

    @PostMapping(path = StratoServices.IDENTITY_SERVICE + "/describe-inherited-tenants")
    DescribeSimpleTenantsResponse describeInheritedTenants(@RequestBody DescribeInheritedTenantsRequest request);

    @PostMapping(path = StratoServices.IDENTITY_SERVICE + "/describe-sub-tenants")
    DescribeSimpleTenantsResponse describeSubTenants(@RequestBody DescribeSubTenantsRequest request);

    @PostMapping(path = StratoServices.IDENTITY_SERVICE + "/describe-tenants")
    Page<NestedTenantResponse> describeTenants(@RequestBody DescribeTenantsRequest request);

    @PostMapping(path = StratoServices.IDENTITY_SERVICE + "/describe-tenants-tree")
    DescribeTenantsTreeResponse describeTenantsTree(@RequestBody DescribeTenantsTreeRequest request);
}
