package com.stratocloud.ip.query;

import com.stratocloud.request.query.NestedAuditable;
import com.stratocloud.ip.ManagedIpState;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NestedIpResponse extends NestedAuditable {
    private Long rangeId;

    private Long ipPoolId;

    private String address;

    private String toBigInteger;

    private ManagedIpState state;

    private Long resourceId;
    private String resourceName;
    private String resourceCategory;

    private String allocationReason;
}
