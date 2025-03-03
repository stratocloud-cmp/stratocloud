package com.stratocloud.provider.tencent.flavor;

import com.stratocloud.exceptions.StratoException;

public record TencentFlavorId(String zone, String instanceType) {

    @Override
    public String toString() {
        return "%s@%s".formatted(instanceType, zone);
    }

    public static TencentFlavorId fromString(String s){
        try {
            String[] arr = s.split("@");
            return new TencentFlavorId(arr[1], arr[0]);
        }catch (Exception e){
            throw new StratoException("Failed to parse TencentFlavorId: "+s);
        }
    }
}
