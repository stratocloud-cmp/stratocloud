package com.stratocloud.resource.query.metadata;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DescribeResourceTypesResponse extends ApiResponse {
    private List<NestedResourceType> resourceTypes;
}
