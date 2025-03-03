package com.stratocloud.resource.query;

import com.stratocloud.request.ApiResponse;
import com.stratocloud.resource.monitor.ResourceQuickStats;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DescribeQuickStatsResponse extends ApiResponse {
    private ResourceQuickStats quickStats;
}
