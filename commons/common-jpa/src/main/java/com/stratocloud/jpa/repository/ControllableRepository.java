package com.stratocloud.jpa.repository;

import com.stratocloud.jpa.entities.Controllable;

import java.util.List;

public interface ControllableRepository<E extends Controllable> extends TenantedRepository<E> {
    List<E> findByOwnerIds(List<Long> ownerIds);

    default boolean transferWhenOwnerDeleted(){
        return true;
    }
}
