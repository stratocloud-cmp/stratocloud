package com.stratocloud.provider.aliyun.eip;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum AliyunBwpChargeType {
    PayByBandwidth(Ids.PAY_BY_BANDWIDTH, Names.PAY_BY_BANDWIDTH),
    PayBy95(Ids.PAY_BY_95, Names.PAY_BY_95),
    PayByDominantTraffic(Ids.PAY_BY_DOMINANT_TRAFFIC, Names.PAY_BY_DOMINANT_TRAFFIC),
    ;


    private final String id;
    private final String name;
    AliyunBwpChargeType(String id, String name){
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }



    public static Optional<AliyunBwpChargeType> fromChargeType(String chargeType){
        return Arrays.stream(AliyunBwpChargeType.values()).filter(
                r -> Objects.equals(chargeType, r.getId())
        ).findAny();
    }

    public static class Ids {
        public static final String PAY_BY_BANDWIDTH = "PayByBandwidth";
        public static final String PAY_BY_95 = "PayBy95";
        public static final String PAY_BY_DOMINANT_TRAFFIC = "PayByDominantTraffic";
    }

    public static class Names {

        public static final String PAY_BY_BANDWIDTH = "按带宽计费";
        public static final String PAY_BY_95 = "按增强型 95 计费";
        public static final String PAY_BY_DOMINANT_TRAFFIC = "按主流量计费";
    }
}
