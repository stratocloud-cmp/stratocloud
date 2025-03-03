package com.stratocloud.workflow.query;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DescribeNodeTypesResponse extends ApiResponse {
    private List<NestedNodeType> nodeTypes;
}
