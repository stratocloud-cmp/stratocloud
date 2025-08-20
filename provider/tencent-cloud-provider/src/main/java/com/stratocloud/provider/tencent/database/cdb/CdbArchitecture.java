package com.stratocloud.provider.tencent.database.cdb;

import com.tencentcloudapi.cdb.v20170320.models.InstanceInfo;

import java.util.Objects;

public enum CdbArchitecture {
    TWO_NODES,
    ECONOMICAL_TWO_NODES,
    THREE_NODES,
    ONE_NODE,
    CLUSTER;

    public static CdbArchitecture fromCdb(InstanceInfo instanceInfo) {
        CdbDeviceType deviceType = CdbDeviceType.fromString(instanceInfo.getDeviceType());

        if(deviceType == CdbDeviceType.CLOUD_NATIVE_CLUSTER ||
                deviceType == CdbDeviceType.CLOUD_NATIVE_CLUSTER_EXCLUSIVE)
            return CLUSTER;
        else if(deviceType == CdbDeviceType.BASIC_V2)
            return ONE_NODE;
        else if(deviceType == CdbDeviceType.ECONOMICAL)
            return ECONOMICAL_TWO_NODES;
        else if(Objects.equals(instanceInfo.getInstanceNodes(), 3L))
            return THREE_NODES;
        else
            return TWO_NODES;
    }
}
