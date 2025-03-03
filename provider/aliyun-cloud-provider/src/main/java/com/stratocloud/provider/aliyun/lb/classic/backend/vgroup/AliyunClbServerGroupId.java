package com.stratocloud.provider.aliyun.lb.classic.backend.vgroup;

import com.stratocloud.exceptions.StratoException;

public record AliyunClbServerGroupId(
        String loadBalancerId,
        String serverGroupId
) {
    @Override
    public String toString() {
        return "%s@%s".formatted(serverGroupId, loadBalancerId);
    }

    public static AliyunClbServerGroupId fromString(String s){
        try {
            String[] arr = s.split("@");
            return new AliyunClbServerGroupId(arr[1], arr[0]);
        }catch (Exception e){
            throw new StratoException("Failed to parse AliyunClbServerGroupId: "+s);
        }
    }
}
