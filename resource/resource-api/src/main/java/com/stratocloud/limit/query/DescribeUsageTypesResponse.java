package com.stratocloud.limit.query;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DescribeUsageTypesResponse extends ApiResponse {
    private List<NestedUsageType> usageTypes;
}
