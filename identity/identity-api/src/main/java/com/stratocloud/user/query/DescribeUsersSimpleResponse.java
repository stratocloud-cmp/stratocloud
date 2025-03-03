package com.stratocloud.user.query;

import com.stratocloud.request.ApiResponse;
import com.stratocloud.identity.SimpleUser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DescribeUsersSimpleResponse extends ApiResponse {
    private List<SimpleUser> users;
}
