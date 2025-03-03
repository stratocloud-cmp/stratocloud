package com.stratocloud.limit;

import com.stratocloud.jpa.entities.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class ResourceUsageLimitTag extends Auditable {
    @ManyToOne
    private ResourceUsageLimit limit;

    @Column(nullable = false)
    private String tagKey;

    @Column(nullable = false)
    private String tagKeyName;

    @Column(nullable = false)
    private String tagValue;

    @Column(nullable = false)
    private String tagValueName;
}
