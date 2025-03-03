package com.stratocloud.jpa.repository;

import com.stratocloud.jpa.entities.Auditable;
import com.stratocloud.jpa.repository.jpa.AuditableJpaRepository;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.List;

public abstract class AbstractAuditableRepository<E extends Auditable, R extends AuditableJpaRepository<E>>
        extends AbstractRepository<E, Long, R> implements AuditableRepository<E>{

    protected AbstractAuditableRepository(R jpaRepository) {
        super(jpaRepository);
    }

    protected Specification<E> getIdSpec(Collection<Long> ids) {
        return (root, query, criteriaBuilder) -> root.get("id").in(ids);
    }
}
