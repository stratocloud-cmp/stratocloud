package com.stratocloud.user.query;

import com.stratocloud.request.query.PagingRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DescribeUsersRequest extends PagingRequest {
    private List<Long> tenantIds;
    private List<Long> userIds;
    private List<Long> roleIds;
    private List<Long> userGroupIds;
    private String search;
    private Boolean disabled;
    private Boolean locked;
}
