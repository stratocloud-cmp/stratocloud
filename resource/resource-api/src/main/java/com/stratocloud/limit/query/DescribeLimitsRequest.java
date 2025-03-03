package com.stratocloud.limit.query;

import com.stratocloud.request.query.PagingRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DescribeLimitsRequest extends PagingRequest {
    private List<Long> tenantIds;

    private String search;
}
