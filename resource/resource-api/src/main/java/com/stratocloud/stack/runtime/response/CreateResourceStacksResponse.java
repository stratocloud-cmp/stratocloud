package com.stratocloud.stack.runtime.response;


import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateResourceStacksResponse extends ApiResponse {
    private List<Long> resourceStackIds;
}
