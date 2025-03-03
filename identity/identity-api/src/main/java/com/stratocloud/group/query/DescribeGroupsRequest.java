package com.stratocloud.group.query;

import com.stratocloud.request.query.PagingRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DescribeGroupsRequest extends PagingRequest {
    private List<Long> userGroupIds;
    private List<Long> userIds;
    private String search;

    private Boolean allGroups = false;
}
