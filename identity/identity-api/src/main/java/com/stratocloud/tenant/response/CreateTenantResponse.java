package com.stratocloud.tenant.response;

import com.stratocloud.request.ApiResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreateTenantResponse extends ApiResponse {
    private Long tenantId;
}
