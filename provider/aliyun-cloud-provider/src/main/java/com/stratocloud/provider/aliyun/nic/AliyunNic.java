package com.stratocloud.provider.aliyun.nic;

import com.aliyun.ecs20140526.models.DescribeNetworkInterfacesResponseBody;
import com.stratocloud.utils.CompareUtil;
import com.stratocloud.utils.Utils;

import java.util.List;
import java.util.Objects;

@SuppressWarnings("Convert2MethodRef")
public record AliyunNic(
        DescribeNetworkInterfacesResponseBody.DescribeNetworkInterfacesResponseBodyNetworkInterfaceSetsNetworkInterfaceSet detail
) {

    public List<String> getPrivateIps(){
        if(detail.getPrivateIpSets() == null || Utils.isEmpty(detail.getPrivateIpSets().getPrivateIpSet()))
            return List.of();

        return detail.getPrivateIpSets().getPrivateIpSet().stream().sorted(
                (ip1, ip2) -> CompareUtil.compareBooleanDesc(ip1.getPrimary(), ip2.getPrimary())
        ).map(
                ip -> ip.getPrivateIpAddress()
        ).toList();
    }

    public List<String> getIpv6List(){
        if(detail.getIpv6Sets() == null || Utils.isEmpty(detail.getIpv6Sets().getIpv6Set()))
            return List.of();

        return detail.getIpv6Sets().getIpv6Set().stream().map(ipv6 -> ipv6.getIpv6Address()).toList();
    }


    public boolean isPrimaryNic(){
        return Objects.equals("Primary", detail().getType());
    }
}
