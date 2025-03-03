package com.stratocloud.limit.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NestedUsageType {
    private String usageType;
    private String usageTypeName;
}
