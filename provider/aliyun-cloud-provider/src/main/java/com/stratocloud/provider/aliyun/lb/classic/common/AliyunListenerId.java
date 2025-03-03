package com.stratocloud.provider.aliyun.lb.classic.common;

import com.stratocloud.exceptions.StratoException;

public record AliyunListenerId(
        String loadBalancerId,
        String protocol,
        String port
) {
    @Override
    public String toString() {
        return "%s:%s:%s".formatted(loadBalancerId, protocol, port);
    }

    public static AliyunListenerId fromString(String s){
        try {
            String[] arr = s.split(":");
            return new AliyunListenerId(arr[0], arr[1], arr[2]);
        }catch (Exception e){
            throw new StratoException("Failed to parse AliyunListenerId: "+s);
        }
    }
}
