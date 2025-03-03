package com.stratocloud.ip.query;

import com.stratocloud.request.query.PagingRequest;
import com.stratocloud.ip.InternetProtocol;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DescribeIpsRequest extends PagingRequest {
    private Long ipPoolId;
    private Long networkResourceId;
    private InternetProtocol protocol;

    private List<String> ips;

    private String search;
}
