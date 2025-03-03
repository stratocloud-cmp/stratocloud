package com.stratocloud.repository;

import com.stratocloud.jpa.repository.AbstractTenantedRepository;
import com.stratocloud.tag.ResourceTagEntry;
import com.stratocloud.tag.ResourceTagValue;
import com.stratocloud.utils.Utils;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TagValueRepositoryImpl extends AbstractTenantedRepository<ResourceTagValue, TagValueJpaRepository> implements TagValueRepository {

    public TagValueRepositoryImpl(TagValueJpaRepository jpaRepository) {
        super(jpaRepository);
    }


    @Override
    public Page<ResourceTagValue> page(String tagEntryKey,
                                       String search,
                                       List<String> tagValues,
                                       Pageable pageable) {
        Specification<ResourceTagValue> spec = getResourceTagValueSpecification(
                tagEntryKey, search, tagValues
        );

        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<ResourceTagValue> getResourceTagValueSpecification(String tagEntryKey,
                                                                             String search,
                                                                             List<String> tagValues) {
        Specification<ResourceTagValue> spec = getCallingTenantSpec();

        spec = spec.and(getTagEntryKeySpec(tagEntryKey));

        if(Utils.isNotBlank(search))
            spec = spec.and(getSearchSpec(search));

        if(Utils.isNotEmpty(tagValues))
            spec = spec.and(getTagValueSpec(tagValues));

        return spec;
    }

    private Specification<ResourceTagValue> getTagValueSpec(List<String> tagValues) {
        return (root, query, criteriaBuilder) -> root.get("tagValue").in(tagValues);
    }

    private Specification<ResourceTagValue> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> {
            Predicate p1 = criteriaBuilder.like(root.get("tagValue"), "%" + search + "%");
            Predicate p2 = criteriaBuilder.like(root.get("tagValueName"), "%" + search + "%");
            return criteriaBuilder.or(p1, p2);
        };
    }

    private Specification<ResourceTagValue> getTagEntryKeySpec(String tagEntryKey) {
        return (root, query, criteriaBuilder) -> {
            Join<ResourceTagEntry, ResourceTagValue> join = root.join("entry");
            return criteriaBuilder.equal(join.get("tagKey"), tagEntryKey);
        };
    }
}
