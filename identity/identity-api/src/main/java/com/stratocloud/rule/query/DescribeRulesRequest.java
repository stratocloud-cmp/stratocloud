package com.stratocloud.rule.query;

import com.stratocloud.request.query.PagingRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DescribeRulesRequest extends PagingRequest {
    private List<Long> tenantIds;
    private List<Long> ruleIds;
    private List<String> ruleTypes;
    private String search;
}
