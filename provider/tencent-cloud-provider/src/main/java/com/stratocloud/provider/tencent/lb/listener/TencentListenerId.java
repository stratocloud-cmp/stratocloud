package com.stratocloud.provider.tencent.lb.listener;

import com.stratocloud.exceptions.StratoException;

public record TencentListenerId(String lbId, String listenerId) {

    @Override
    public String toString() {
        return "%s@%s".formatted(listenerId, lbId);
    }

    public static TencentListenerId fromString(String s){
        try {
            String[] arr = s.split("@");
            return new TencentListenerId(arr[1], arr[0]);
        }catch (Exception e){
            throw new StratoException("Failed to parse TencentListenerId: "+s);
        }
    }
}
