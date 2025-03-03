package com.stratocloud.provider.aliyun.lb.classic.backend.msgroup;

import com.stratocloud.exceptions.StratoException;

public record AliyunClbMasterSlaveGroupId(
        String loadBalancerId,
        String masterSlaveGroupId
) {
    @Override
    public String toString() {
        return "%s@%s".formatted(masterSlaveGroupId, loadBalancerId);
    }

    public static AliyunClbMasterSlaveGroupId fromString(String s){
        try {
            String[] arr = s.split("@");
            return new AliyunClbMasterSlaveGroupId(arr[1], arr[0]);
        }catch (Exception e){
            throw new StratoException("Failed to parse AliyunClbMasterSlaveGroupId: "+s);
        }
    }
}
