package com.stratocloud.external.resource;

import com.stratocloud.identity.SimpleTenant;
import com.stratocloud.tenant.TenantApi;
import com.stratocloud.tenant.query.DescribeInheritedTenantsRequest;
import com.stratocloud.tenant.query.DescribeSimpleTenantsResponse;
import com.stratocloud.tenant.query.DescribeSubTenantsRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TenantGatewayServiceImpl implements TenantGatewayService{

    private final TenantApi tenantApi;

    public TenantGatewayServiceImpl(TenantApi tenantApi) {
        this.tenantApi = tenantApi;
    }

    @Override
    public List<SimpleTenant> findInheritedTenants(Long tenantId) {
        DescribeInheritedTenantsRequest request = new DescribeInheritedTenantsRequest();
        request.setTenantId(tenantId);

        DescribeSimpleTenantsResponse response = tenantApi.describeInheritedTenants(request);
        return response.getTenants();
    }

    @Override
    public List<SimpleTenant> findSubTenants(Long tenantId) {
        DescribeSubTenantsRequest request = new DescribeSubTenantsRequest();
        request.setTenantId(tenantId);
        DescribeSimpleTenantsResponse response = tenantApi.describeSubTenants(request);
        return response.getTenants();
    }
}
