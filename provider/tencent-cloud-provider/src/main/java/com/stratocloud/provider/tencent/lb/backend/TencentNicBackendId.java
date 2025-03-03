package com.stratocloud.provider.tencent.lb.backend;

import com.stratocloud.exceptions.StratoException;

public record TencentNicBackendId(String lbId, String listenerId, String ip) {

    @Override
    public String toString() {
        return "%s@%s@%s".formatted(ip, listenerId, lbId);
    }

    public static TencentNicBackendId fromString(String s){
        try {
            String[] arr = s.split("@");
            return new TencentNicBackendId(arr[2], arr[1], arr[0]);
        }catch (Exception e){
            throw new StratoException("Failed to parse TencentNicBackendId: "+s);
        }
    }
}
