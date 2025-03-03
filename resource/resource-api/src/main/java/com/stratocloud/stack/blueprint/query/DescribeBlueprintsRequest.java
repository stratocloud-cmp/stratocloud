package com.stratocloud.stack.blueprint.query;

import com.stratocloud.request.query.PagingRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DescribeBlueprintsRequest extends PagingRequest {
    private String search;
    private List<Long> blueprintIds;
}
