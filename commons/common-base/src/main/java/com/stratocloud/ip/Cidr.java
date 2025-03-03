package com.stratocloud.ip;


import com.stratocloud.exceptions.InvalidArgumentException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.utils.NetworkUtil;

public record Cidr(String value) {
    public Cidr {
        if (!NetworkUtil.isValidCidr(value))
            throw new InvalidArgumentException("无效CIDR");
    }

    public IpAddress getAddress(){
        String ip = value.split("/")[0];
        return new IpAddress(ip);
    }

    public int getPrefixLength(){
        String len = value.split("/")[1];
        return Integer.parseInt(len);
    }

    public String getIpv4SubnetMask(){
        if(getAddress().getProtocol() != InternetProtocol.IPv4)
            throw new StratoException("Cannot convert cidr %s to ipv4 subnet mask.".formatted(value));

        return NetworkUtil.getIpv4Mask(getPrefixLength());
    }
}
