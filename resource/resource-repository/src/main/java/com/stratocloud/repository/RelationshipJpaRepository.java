package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.AuditableJpaRepository;
import com.stratocloud.resource.Relationship;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RelationshipJpaRepository
        extends AuditableJpaRepository<Relationship>, JpaSpecificationExecutor<Relationship> {
}
