package com.stratocloud.notification.query;

import com.stratocloud.request.query.PagingRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DescribeNotificationPoliciesRequest extends PagingRequest {
    private String search;
}
