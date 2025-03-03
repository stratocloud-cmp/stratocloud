package com.stratocloud.order.query;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DescribeRollbackTargetsResponse extends ApiResponse {
    private List<NestedRollbackTarget> targets;
}
