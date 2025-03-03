package com.stratocloud.resource;

import com.stratocloud.jpa.entities.Auditable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter(AccessLevel.PROTECTED)
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AllocatedResourceUsage extends Auditable {
    @ManyToOne
    private Resource resource;

    @Embedded
    private ResourceUsage resourceUsage;


    public AllocatedResourceUsage(ResourceUsage resourceUsage) {
        this.resourceUsage = resourceUsage;
    }
}
