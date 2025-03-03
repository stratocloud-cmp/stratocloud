package com.stratocloud.resource;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ResourceUsage {
    @Column(nullable = false)
    private String usageType;
    @Column(nullable = false)
    private BigDecimal usageValue;
}
