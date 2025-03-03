package com.stratocloud.ip.response;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CreateIpPoolResponse extends ApiResponse {
    private Long ipPoolId;
}
