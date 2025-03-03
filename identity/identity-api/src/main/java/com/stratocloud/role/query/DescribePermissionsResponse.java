package com.stratocloud.role.query;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DescribePermissionsResponse extends ApiResponse {
    private List<NestedPermission> permissions;
}
