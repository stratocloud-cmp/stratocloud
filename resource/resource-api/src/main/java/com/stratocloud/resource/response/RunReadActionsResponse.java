package com.stratocloud.resource.response;

import com.stratocloud.request.ApiResponse;
import com.stratocloud.resource.ResourceReadActionResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RunReadActionsResponse extends ApiResponse {

    private List<NestedReadActionResponse> responseList;

    @Data
    public static class NestedReadActionResponse {
        private Long resourceId;
        private String resourceName;
        private List<ResourceReadActionResult> resultList;
    }
}
