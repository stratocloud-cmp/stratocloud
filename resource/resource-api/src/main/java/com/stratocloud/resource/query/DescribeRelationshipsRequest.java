package com.stratocloud.resource.query;

import com.stratocloud.request.query.PagingRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DescribeRelationshipsRequest extends PagingRequest {
    private List<Long> relationshipIds;
}
