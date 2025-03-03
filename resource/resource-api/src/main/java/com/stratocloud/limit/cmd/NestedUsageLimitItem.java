package com.stratocloud.limit.cmd;

import lombok.Data;

@Data
public class NestedUsageLimitItem {
    private String usageType;
    private String usageTypeName;
    private String limitValue;
    private String usageValue;

    private Integer percentage;
}
