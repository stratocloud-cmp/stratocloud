package com.stratocloud.group.query;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DescribeSimpleGroupsResponse extends ApiResponse {
    private List<SimpleUserGroup> groups;
}
