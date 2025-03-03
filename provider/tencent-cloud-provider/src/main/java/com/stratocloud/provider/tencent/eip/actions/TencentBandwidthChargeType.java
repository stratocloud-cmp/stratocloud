package com.stratocloud.provider.tencent.eip.actions;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum TencentBandwidthChargeType {
    TOP5_POSTPAID_BY_MONTH(Ids.TOP5_POSTPAID_BY_MONTH, Names.TOP5_POSTPAID_BY_MONTH),
    PERCENT95_POSTPAID_BY_MONTH(Ids.PERCENT95_POSTPAID_BY_MONTH, Names.PERCENT95_POSTPAID_BY_MONTH),
    ENHANCED95_POSTPAID_BY_MONTH(Ids.ENHANCED95_POSTPAID_BY_MONTH, Names.ENHANCED95_POSTPAID_BY_MONTH),
    FIXED_PREPAID_BY_MONTH(Ids.FIXED_PREPAID_BY_MONTH, Names.FIXED_PREPAID_BY_MONTH),
    PEAK_BANDWIDTH_POSTPAID_BY_DAY(Ids.PEAK_BANDWIDTH_POSTPAID_BY_DAY, Names.PEAK_BANDWIDTH_POSTPAID_BY_DAY),
    ;


    private final String id;
    private final String name;
    TencentBandwidthChargeType(String id, String name){
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }



    public static Optional<TencentBandwidthChargeType> fromChargeType(String chargeType){
        return Arrays.stream(TencentBandwidthChargeType.values()).filter(
                r -> Objects.equals(chargeType, r.getId())
        ).findAny();
    }

    public static class Ids {
        public static final String TOP5_POSTPAID_BY_MONTH = "TOP5_POSTPAID_BY_MONTH";
        public static final String PERCENT95_POSTPAID_BY_MONTH = "PERCENT95_POSTPAID_BY_MONTH";
        public static final String ENHANCED95_POSTPAID_BY_MONTH = "ENHANCED95_POSTPAID_BY_MONTH";
        public static final String FIXED_PREPAID_BY_MONTH = "FIXED_PREPAID_BY_MONTH";
        public static final String PEAK_BANDWIDTH_POSTPAID_BY_DAY = "PEAK_BANDWIDTH_POSTPAID_BY_DAY";
    }

    public static class Names {
        public static final String TOP5_POSTPAID_BY_MONTH = "按月后付费TOP5计费";
        public static final String PERCENT95_POSTPAID_BY_MONTH = "按月后付费月95计费";
        public static final String ENHANCED95_POSTPAID_BY_MONTH = "按月后付费增强型95计费";
        public static final String FIXED_PREPAID_BY_MONTH = "包月预付费计费";
        public static final String PEAK_BANDWIDTH_POSTPAID_BY_DAY = "后付费日结按带宽计费";

    }
}
