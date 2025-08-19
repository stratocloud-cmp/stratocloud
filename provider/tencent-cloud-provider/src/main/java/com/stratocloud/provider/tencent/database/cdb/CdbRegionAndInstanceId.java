package com.stratocloud.provider.tencent.database.cdb;

import com.stratocloud.exceptions.StratoException;

public record CdbRegionAndInstanceId(String regionId, String instanceId) {

    @Override
    public String toString() {
        return "%s@%s".formatted(instanceId, regionId);
    }

    public static CdbRegionAndInstanceId fromString(String s){
        try {
            String[] arr = s.split("@");
            return new CdbRegionAndInstanceId(arr[1], arr[0]);
        }catch (Exception e){
            throw new StratoException("Failed to parse CdbRegionAndInstanceId: "+s);
        }
    }
}
