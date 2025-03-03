package com.stratocloud.repository;

import com.stratocloud.jpa.repository.AuditableRepository;
import com.stratocloud.resource.Relationship;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RelationshipRepository extends AuditableRepository<Relationship> {
    Relationship findRelationship(Long id);

    Page<Relationship> pageRequirements(Long sourceId, String relationshipType, String search, Pageable pageable);

    Page<Relationship> pageCapabilities(Long targetId, String relationshipType, String search, Pageable pageable);

    Page<Relationship> page(List<Long> relationshipIds, Pageable pageable);
}
