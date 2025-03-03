package com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup;

import com.stratocloud.exceptions.StratoException;

public record AliyunClbBackendId(
        String loadBalancerId,
        String resourceType,
        String resourceId,
        Integer weight
) {
    @Override
    public String toString() {
        return "%s:%s:%s:%s".formatted(loadBalancerId, resourceType, resourceId, weight);
    }

    public static AliyunClbBackendId fromString(String s){
        try {
            String[] arr = s.split(":");
            return new AliyunClbBackendId(arr[0], arr[1], arr[2], Integer.parseInt(arr[3]));
        }catch (Exception e){
            throw new StratoException("Failed to parse AliyunClbBackendId: "+s);
        }
    }
}
