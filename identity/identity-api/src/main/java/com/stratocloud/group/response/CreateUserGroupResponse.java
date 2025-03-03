package com.stratocloud.group.response;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CreateUserGroupResponse extends ApiResponse {
    private Long userGroupId;
}
