package com.stratocloud.jpa.repository.jpa;

import com.stratocloud.jpa.entities.Controllable;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface ControllableJpaRepository<E extends Controllable> extends TenantedJpaRepository<E>{
    List<E> findByOwnerIdIn(List<Long> ownerIds);
}
