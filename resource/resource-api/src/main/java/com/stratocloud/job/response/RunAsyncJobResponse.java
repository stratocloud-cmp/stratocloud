package com.stratocloud.job.response;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RunAsyncJobResponse extends ApiResponse {
    private Long jobId;
}
