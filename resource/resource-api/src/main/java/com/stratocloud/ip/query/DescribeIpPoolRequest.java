package com.stratocloud.ip.query;

import com.stratocloud.request.query.PagingRequest;
import com.stratocloud.ip.InternetProtocol;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DescribeIpPoolRequest extends PagingRequest {
    private List<Long> ipPoolIds;
    private List<Long> networkResourceIds;
    private String search;
    private InternetProtocol protocol;
}
