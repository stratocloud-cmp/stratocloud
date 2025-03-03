package com.stratocloud.resource.query.metadata;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DescribeRelationshipSpecResponse extends ApiResponse {
    private NestedRelationshipSpec spec;
}
