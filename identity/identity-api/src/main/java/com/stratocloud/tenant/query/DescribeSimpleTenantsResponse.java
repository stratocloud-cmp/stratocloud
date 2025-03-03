package com.stratocloud.tenant.query;

import com.stratocloud.request.ApiResponse;
import com.stratocloud.identity.SimpleTenant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DescribeSimpleTenantsResponse extends ApiResponse {
    private List<SimpleTenant> tenants;
}
