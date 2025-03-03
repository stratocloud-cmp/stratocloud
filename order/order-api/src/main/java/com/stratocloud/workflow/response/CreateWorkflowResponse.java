package com.stratocloud.workflow.response;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CreateWorkflowResponse extends ApiResponse {
    private Long workflowId;
}
