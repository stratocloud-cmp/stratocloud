package com.stratocloud.resource.query;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class NestedResourceUsage {
    private String usageType;
    private String usageTypeName;
    private BigDecimal usageValue;
}
