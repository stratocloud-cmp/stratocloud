package com.stratocloud.tenant.query;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DescribeTenantsTreeResponse extends ApiResponse {
    private List<NestedTenantsTreeNode> roots;
}
