package com.stratocloud.provider.tencent.disk.actions;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum TencentDiskType {
    LOCAL_BASIC(Ids.LOCAL_BASIC, Names.LOCAL_BASIC),
    LOCAL_SSD(Ids.LOCAL_SSD, Names.LOCAL_SSD),
    CLOUD_BASIC(Ids.CLOUD_BASIC, Names.CLOUD_BASIC),
    CLOUD_SSD(Ids.CLOUD_SSD, Names.CLOUD_SSD),
    CLOUD_PREMIUM(Ids.CLOUD_PREMIUM, Names.CLOUD_PREMIUM),
    CLOUD_BSSD(Ids.CLOUD_BSSD, Names.CLOUD_BSSD),
    CLOUD_HSSD(Ids.CLOUD_HSSD, Names.CLOUD_HSSD),
    CLOUD_TSSD(Ids.CLOUD_TSSD, Names.CLOUD_TSSD),
    ;


    private final String id;
    private final String name;
    TencentDiskType(String id, String name){
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }



    public static Optional<TencentDiskType> fromId(String diskType){
        return Arrays.stream(TencentDiskType.values()).filter(
                r -> Objects.equals(diskType, r.getId())
        ).findAny();
    }

    public static class Ids {
        public static final String LOCAL_BASIC = "LOCAL_BASIC";
        public static final String LOCAL_SSD = "LOCAL_SSD";
        public static final String CLOUD_BASIC = "CLOUD_BASIC";
        public static final String CLOUD_SSD = "CLOUD_SSD";
        public static final String CLOUD_PREMIUM = "CLOUD_PREMIUM";
        public static final String CLOUD_BSSD = "CLOUD_BSSD";
        public static final String CLOUD_HSSD = "CLOUD_HSSD";
        public static final String CLOUD_TSSD = "CLOUD_TSSD";
    }

    public static class Names {
        public static final String LOCAL_BASIC = "本地硬盘";
        public static final String LOCAL_SSD = "本地SSD硬盘";
        public static final String CLOUD_BASIC = "普通云硬盘";
        public static final String CLOUD_SSD = "SSD云硬盘";
        public static final String CLOUD_PREMIUM = "高性能云硬盘";
        public static final String CLOUD_BSSD = "通用型SSD云硬盘";
        public static final String CLOUD_HSSD = "增强型SSD云硬盘";
        public static final String CLOUD_TSSD = "极速型SSD云硬盘";
    }
}
