package com.stratocloud.resource.query.metadata;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DescribeRelationshipTypesResponse extends ApiResponse {
    private List<NestedRelationshipSpec> relationshipSpecs;
}
