package com.stratocloud.repository;

import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.jpa.repository.AbstractAuditableRepository;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.RelationshipState;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.Utils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class RelationshipRepositoryImpl extends AbstractAuditableRepository<Relationship, RelationshipJpaRepository>
        implements RelationshipRepository {

    private final ResourceRepositoryImpl resourceRepository;

    public RelationshipRepositoryImpl(RelationshipJpaRepository jpaRepository,
                                      ResourceRepositoryImpl resourceRepository) {
        super(jpaRepository);
        this.resourceRepository = resourceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Relationship findRelationship(Long id) {
        return jpaRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Relationship not found.")
        );
    }

    @Override
    public void validatePermission(Relationship entity) {
        resourceRepository.validatePermission(entity.getSource());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Relationship> pageRequirements(Long sourceId,
                                               String relationshipType,
                                               String search,
                                               Pageable pageable) {
        Specification<Relationship> spec = getSpec();

        spec = spec.and(getRequirementSpec(sourceId, relationshipType, search));

        return jpaRepository.findAll(spec, pageable);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<Relationship> pageCapabilities(Long targetId,
                                               String relationshipType,
                                               String search,
                                               Pageable pageable) {
        Resource resource = resourceRepository.findResource(targetId);

        resourceRepository.validatePermission(resource);

        Specification<Relationship> spec = getSpec();

        spec = spec.and(getCapabilitySpec(targetId, relationshipType, search));

        return jpaRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Relationship> page(List<Long> relationshipIds, Pageable pageable) {
        Specification<Relationship> spec = getSpec();

        if(Utils.isNotEmpty(relationshipIds))
            spec = spec.and(getIdSpec(relationshipIds));

        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<Relationship> getCapabilitySpec(Long targetId, String relationshipType, String search) {
        return (root, query, criteriaBuilder) -> {
            Predicate p1 = criteriaBuilder.equal(root.get("target").get("id"), targetId);
            Predicate p2 = criteriaBuilder.equal(root.get("type"), relationshipType);

            Predicate p3 = criteriaBuilder.notEqual(root.get("state"), RelationshipState.DISCONNECTED);

            if(Utils.isNotBlank(search)){
                Predicate p4 = criteriaBuilder.like(
                        root.join("source").get("name"),
                        "%" + search + "%"
                );

                return criteriaBuilder.and(p1, p2, p3, p4);
            }else {
                return criteriaBuilder.and(p1, p2, p3);
            }
        };
    }

    private Specification<Relationship> getRequirementSpec(Long sourceId,
                                                           String relationshipType,
                                                           String search) {
        return (root, query, criteriaBuilder) -> {
            Predicate p1 = criteriaBuilder.equal(root.get("source").get("id"), sourceId);
            Predicate p2 = criteriaBuilder.equal(root.get("type"), relationshipType);

            Predicate p3 = criteriaBuilder.notEqual(root.get("state"), RelationshipState.DISCONNECTED);

            if(Utils.isNotBlank(search)){
                Predicate p4 = criteriaBuilder.like(
                        root.join("target").get("name"),
                        "%" + search + "%"
                );

                return criteriaBuilder.and(p1, p2, p3, p4);
            }else {
                return criteriaBuilder.and(p1, p2, p3);
            }
        };
    }


}
