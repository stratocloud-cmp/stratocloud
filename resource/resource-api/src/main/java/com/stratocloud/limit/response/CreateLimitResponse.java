package com.stratocloud.limit.response;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreateLimitResponse extends ApiResponse {
    private Long limitId;
}
