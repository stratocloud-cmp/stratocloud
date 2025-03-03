package com.stratocloud.jpa.repository;

import com.stratocloud.jpa.entities.Tenanted;

import java.util.List;

public interface TenantedRepository<E extends Tenanted> extends AuditableRepository<E> {
    List<E> findAllByTenantIds(List<Long> tenantIds);

    boolean existsByTenantIds(List<Long> tenantIds);

    default boolean transferWhenTenantDeleted(){
        return true;
    }
}
