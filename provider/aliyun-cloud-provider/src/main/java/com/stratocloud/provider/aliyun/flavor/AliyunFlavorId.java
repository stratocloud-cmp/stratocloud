package com.stratocloud.provider.aliyun.flavor;

import com.stratocloud.exceptions.StratoException;

public record AliyunFlavorId(String zoneId, String instanceTypeId) {

    @Override
    public String toString() {
        return "%s@%s".formatted(instanceTypeId, zoneId);
    }

    public static AliyunFlavorId fromString(String s){
        try {
            String[] arr = s.split("@");
            return new AliyunFlavorId(arr[1], arr[0]);
        }catch (Exception e){
            throw new StratoException("Failed to parse AliyunFlavorId: "+s);
        }
    }
}
