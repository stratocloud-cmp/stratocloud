package com.stratocloud.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class Tenanted extends Auditable {
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public void transferToNewTenant(Long tenantId) {
        setTenantId(tenantId);
    }
}
