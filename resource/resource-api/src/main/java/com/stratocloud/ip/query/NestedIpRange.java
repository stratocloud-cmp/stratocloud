package com.stratocloud.ip.query;

import com.stratocloud.request.query.NestedAuditable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NestedIpRange extends NestedAuditable {
    private String startIp;
    private String endIp;
}
