package com.stratocloud.provider.tencent.database.cdb;

import lombok.Getter;

import java.util.Objects;

@Getter
public enum CdbDeviceType {
    UNIVERSAL(Constants.UNIVERSAL_ID, Constants.UNIVERSAL_LABEL),
    EXCLUSIVE(Constants.EXCLUSIVE_ID, Constants.EXCLUSIVE_LABEL),
    BASIC_V2(Constants.BASIC_V2_ID, Constants.BASIC_V2_LABEL),
    CLOUD_NATIVE_CLUSTER(Constants.CLOUD_NATIVE_CLUSTER_ID, Constants.CLOUD_NATIVE_CLUSTER_LABEL),
    CLOUD_NATIVE_CLUSTER_EXCLUSIVE(Constants.CLOUD_NATIVE_CLUSTER_EXCLUSIVE_ID, Constants.CLOUD_NATIVE_CLUSTER_EXCLUSIVE_LABEL),
    ECONOMICAL(Constants.ECONOMICAL_ID, Constants.ECONOMICAL_LABEL),
    UNKNOWN("UNKNOWN", "未知");

    private final String id;
    private final String label;

    CdbDeviceType(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static CdbDeviceType fromString(String s){
        for (CdbDeviceType type : values()) {
            if(Objects.equals(type.id, s))
                return type;
        }
        return UNKNOWN;
    }

    public static class Constants {
        public static final String UNIVERSAL_ID = "UNIVERSAL";
        public static final String EXCLUSIVE_ID = "EXCLUSIVE";
        public static final String BASIC_V2_ID = "BASIC_V2";
        public static final String CLOUD_NATIVE_CLUSTER_ID = "CLOUD_NATIVE_CLUSTER";
        public static final String CLOUD_NATIVE_CLUSTER_EXCLUSIVE_ID = "CLOUD_NATIVE_CLUSTER_EXCLUSIVE";
        public static final String ECONOMICAL_ID = "ECONOMICAL";

        public static final String UNIVERSAL_LABEL = "通用型";
        public static final String EXCLUSIVE_LABEL = "独享型";
        public static final String BASIC_V2_LABEL = "基础型";
        public static final String CLOUD_NATIVE_CLUSTER_LABEL = "集群版标准型";
        public static final String CLOUD_NATIVE_CLUSTER_EXCLUSIVE_LABEL = "集群版加强型";
        public static final String ECONOMICAL_LABEL = "经济型";
    }
}
