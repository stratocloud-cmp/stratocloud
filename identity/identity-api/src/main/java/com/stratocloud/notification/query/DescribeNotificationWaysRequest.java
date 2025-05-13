package com.stratocloud.notification.query;

import com.stratocloud.request.query.PagingRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DescribeNotificationWaysRequest extends PagingRequest {
    private String search;
    private List<Long> notificationWayIds;
}
