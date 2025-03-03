package com.stratocloud.resource.query;

import com.stratocloud.request.ApiResponse;
import com.stratocloud.resource.ResourceAction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DescribeAvailableActionsResponse extends ApiResponse {
    private List<ResourceAction> allActions;
    private List<ResourceAction> availableActions;
    private List<ResourceAction> readActions;
}
