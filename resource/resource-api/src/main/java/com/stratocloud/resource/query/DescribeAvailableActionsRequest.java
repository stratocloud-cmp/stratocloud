package com.stratocloud.resource.query;

import com.stratocloud.request.query.QueryRequest;
import lombok.Data;

import java.util.List;

@Data
public class DescribeAvailableActionsRequest implements QueryRequest {
    private List<Long> resourceIds;
    private String category;
}
