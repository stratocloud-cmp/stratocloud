package com.stratocloud.limit;

import com.stratocloud.jpa.entities.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
public class ResourceUsageLimitItem extends Auditable {
    @Column(nullable = false)
    private String usageType;
    @Column(nullable = false)
    private String usageTypeName;
    @Column(nullable = false)
    private BigDecimal limitValue;
    @Column(nullable = false)
    private BigDecimal usageValue = BigDecimal.ZERO;

    @ManyToOne
    private ResourceUsageLimit limit;
}
