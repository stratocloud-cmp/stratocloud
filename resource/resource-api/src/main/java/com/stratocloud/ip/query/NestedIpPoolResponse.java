package com.stratocloud.ip.query;

import com.stratocloud.request.query.NestedTenanted;
import com.stratocloud.ip.InternetProtocol;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NestedIpPoolResponse extends NestedTenanted {
    private String name;
    private String description;
    private InternetProtocol protocol;
    private String cidr;
    private String gateway;

    private List<NestedIpRange> ranges;
    private List<NestedAttachedNetworkResource> attachedNetworkResources;
}
