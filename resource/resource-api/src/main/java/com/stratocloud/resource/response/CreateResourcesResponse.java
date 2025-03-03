package com.stratocloud.resource.response;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CreateResourcesResponse extends ApiResponse {
    private List<Long> resourceIds;
}
