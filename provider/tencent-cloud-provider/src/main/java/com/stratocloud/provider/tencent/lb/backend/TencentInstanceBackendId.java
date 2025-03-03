package com.stratocloud.provider.tencent.lb.backend;

import com.stratocloud.exceptions.StratoException;

public record TencentInstanceBackendId(String lbId, String listenerId, String instanceId) {

    @Override
    public String toString() {
        return "%s@%s@%s".formatted(instanceId, listenerId, lbId);
    }

    public static TencentInstanceBackendId fromString(String s){
        try {
            String[] arr = s.split("@");
            return new TencentInstanceBackendId(arr[2], arr[1], arr[0]);
        }catch (Exception e){
            throw new StratoException("Failed to parse TencentInstanceBackendId: "+s);
        }
    }
}
