package com.stratocloud.resource;

import com.stratocloud.jpa.entities.Auditable;
import jakarta.persistence.Column;
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
public class PreAllocatedResourceUsage extends Auditable {
    @ManyToOne
    private Resource resource;
    @Embedded
    private ResourceUsage resourceUsage;
    @Column(nullable = false)
    private Long taskId;

    public PreAllocatedResourceUsage(ResourceUsage resourceUsage, Long taskId) {
        this.resourceUsage = resourceUsage;
        this.taskId = taskId;
    }
}
