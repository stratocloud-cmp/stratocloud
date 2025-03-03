package com.stratocloud.ip;


import com.googlecode.ipv6.IPv6Address;
import com.stratocloud.exceptions.InvalidArgumentException;
import com.stratocloud.utils.NetworkUtil;

import java.math.BigInteger;

public record IpAddress(String address) {
    public IpAddress{
        if (address == null) {
            throw new InvalidArgumentException("Ip address cannot be null.");
        }

        if (!NetworkUtil.isValidIp(address)) {
            throw new InvalidArgumentException("Invalid ip address: " + address);
        }
    }

    public boolean isValid(InternetProtocol internetProtocol){
        if(internetProtocol == InternetProtocol.IPv4){
            return isValidIpv4();
        }else if(internetProtocol == InternetProtocol.IPv6){
            return isValidIpv6();
        }else {
            return isValidIpv4();
        }
    }

    public boolean isValidIpv4() {
        return NetworkUtil.isValidIpv4(address);
    }

    public boolean isValidIpv6() {
        return NetworkUtil.isValidIpv6(address);
    }

    public InternetProtocol getProtocol() {
        if (isValidIpv4()) {
            return InternetProtocol.IPv4;
        }
        if (isValidIpv6()) {
            return InternetProtocol.IPv6;
        }
        throw new IllegalStateException();
    }

    public BigInteger toBigInteger() {
        InternetProtocol protocol = getProtocol();
        if (protocol == InternetProtocol.IPv4) {
            return BigInteger.valueOf(NetworkUtil.ip2Long(address));
        }
        if (protocol == InternetProtocol.IPv6) {
            return IPv6Address.fromString(address).toBigInteger();
        }
        throw new IllegalStateException();
    }

    public static IpAddress fromBigInteger(BigInteger bigInteger, InternetProtocol protocol) {
        if (protocol == InternetProtocol.IPv4) {
            return new IpAddress(NetworkUtil.long2Ip(bigInteger.longValueExact()));
        }
        if (protocol == InternetProtocol.IPv6) {
            return new IpAddress(IPv6Address.fromBigInteger(bigInteger).toString());
        }
        throw new IllegalArgumentException("Unknown protocol: " + protocol);
    }

    @Override
    public String toString() {
        return address;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IpAddress that) {
            return toBigInteger().equals(that.toBigInteger());
        }
        return false;
    }

    public boolean isReachable(){
        return NetworkUtil.isIpReachable(address);
    }
}
