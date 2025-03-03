package com.stratocloud.account.query;

import com.stratocloud.request.query.PagingRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DescribeAccountsRequest extends PagingRequest {
    private List<Long> accountIds;
    private String resourceCategory;
    private List<String> providerIds;
    private String search;
    private Boolean disabled;
}
