package com.stratocloud.provider.tencent.rocketmq;

import com.tencentcloudapi.trocket.v20230308.models.ProductSKU;

public class RocketUtil {
    public static String formatSkuName(ProductSKU sku) {
        String instanceTypeName = switch (sku.getInstanceType()){
            case "EXPERIMENT" -> "体验版";
            case "BASIC" -> "基础版";
            case "PRO" -> "专业版";
            case "PLATINUM" -> "白金版";
            default -> sku.getInstanceType();
        };

        return "%s Topic数量:%s Tps上限:%s".formatted(
                instanceTypeName,
                sku.getTopicNumLimit(),
                sku.getTpsLimit()
        );
    }

    public static int getSkuInstanceTypePriority(ProductSKU sku){
        if(sku.getInstanceType() == null)
            return Integer.MAX_VALUE;
        return switch (sku.getInstanceType()){
            case "EXPERIMENT" -> 4;
            case "BASIC" -> 1;
            case "PRO" -> 2;
            case "PLATINUM" -> 3;
            default -> Integer.MAX_VALUE;
        };
    }
}
