package com.stratocloud.notification.query;

import com.stratocloud.request.query.PagingRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DescribeInternalMailsRequest extends PagingRequest {
    private String search;
    private Boolean read;
}
