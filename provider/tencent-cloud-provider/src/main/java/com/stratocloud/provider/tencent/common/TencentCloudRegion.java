package com.stratocloud.provider.tencent.common;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum TencentCloudRegion {
    BEIJING(Ids.AP_BEIJING, Names.AP_BEIJING),
    GUANGZHOU(Ids.AP_GUANGZHOU, Names.AP_GUANGZHOU),
    HONGKONG(Ids.AP_HONGKONG, Names.AP_HONGKONG),
    SHANGHAI(Ids.AP_SHANGHAI, Names.AP_SHANGHAI),
    SHANGHAI_FSI(Ids.AP_SHANGHAI_FSI, Names.AP_SHANGHAI_FSI),
    SHENZHEN_FSI(Ids.AP_SHENZHEN_FSI, Names.AP_SHENZHEN_FSI),
    SINGAPORE(Ids.AP_SINGAPORE, Names.AP_SINGAPORE),
    SILICON_VALLEY(Ids.NA_SILICONVALLEY, Names.NA_SILICONVALLEY),
    TORONTO(Ids.NA_TORONTO, Names.NA_TORONTO),
    ;


    private final String id;
    private final String name;
    TencentCloudRegion(String id, String name){
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }



    public static Optional<TencentCloudRegion> fromId(String regionId){
        return Arrays.stream(TencentCloudRegion.values()).filter(
                r -> Objects.equals(regionId, r.getId())
        ).findAny();
    }

    public static class Ids {
        public static final String AP_BEIJING = "ap-beijing";
        public static final String AP_GUANGZHOU = "ap-guangzhou";
        public static final String AP_HONGKONG = "ap-hongkong";
        public static final String AP_SHANGHAI = "ap-shanghai";
        public static final String AP_SHANGHAI_FSI = "ap-shanghai-fsi";
        public static final String AP_SHENZHEN_FSI = "ap-shenzhen-fsi";
        public static final String AP_SINGAPORE = "ap-singapore";
        public static final String NA_SILICONVALLEY = "na-siliconvalley";
        public static final String NA_TORONTO = "na-toronto";
    }

    public static class Names {
        public static final String AP_BEIJING = "华北地区(北京)";
        public static final String AP_GUANGZHOU = "华南地区(广州)";
        public static final String AP_HONGKONG = "港澳台地区(中国香港)";
        public static final String AP_SHANGHAI = "华东地区(上海)";
        public static final String AP_SHANGHAI_FSI = "华东地区(上海金融)";
        public static final String AP_SHENZHEN_FSI = "华南地区(深圳金融)";
        public static final String AP_SINGAPORE = "亚太东南(新加坡)";
        public static final String NA_SILICONVALLEY = "美国西部(硅谷)";
        public static final String NA_TORONTO = "北美地区(多伦多)";
    }
}
