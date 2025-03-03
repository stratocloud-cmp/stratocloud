package com.stratocloud.role.query;

import com.stratocloud.identity.RoleType;
import com.stratocloud.request.query.PagingRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DescribeRolesRequest extends PagingRequest {
    private List<Long> roleIds;
    private List<RoleType> roleTypes;
    private String search;
    private List<Long> userIds;
}
