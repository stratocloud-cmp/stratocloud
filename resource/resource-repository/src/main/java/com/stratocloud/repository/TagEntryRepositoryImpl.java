package com.stratocloud.repository;

import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.jpa.repository.AbstractTenantedRepository;
import com.stratocloud.tag.ResourceTagEntry;
import com.stratocloud.tag.TagEntryFilters;
import com.stratocloud.utils.Utils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class TagEntryRepositoryImpl extends AbstractTenantedRepository<ResourceTagEntry, TagEntryJpaRepository>
        implements TagEntryRepository{

    public TagEntryRepositoryImpl(TagEntryJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceTagEntry findTagEntry(Long tagEntryId) {
        return jpaRepository.findById(tagEntryId).orElseThrow(
                () -> new EntityNotFoundException("Resource tag entry not found.")
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResourceTagEntry> findByTagKey(String tagKey) {
        return jpaRepository.findByTagKeyIs(tagKey);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResourceTagEntry> page(TagEntryFilters filters, Boolean disabled, Pageable pageable) {
        Specification<ResourceTagEntry> spec = getTagEntrySpecification(filters, disabled);

        return jpaRepository.findAll(spec, pageable);
    }



    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureTagValue(String category, String key, String keyName, String value, String valueName, int index) {
        Optional<ResourceTagEntry> optional = jpaRepository.findByTagKey(key);

        ResourceTagEntry tagEntry;

        tagEntry = optional.orElseGet(() -> ResourceTagEntry.builder()
                .resourceCategory(category)
                .tagKey(key).tagKeyName(keyName)
                .requiredWhenCreating(true)
                .requiredWhenFiltering(true)
                .userGroupTaggable(false)
                .build());

        tagEntry.addValue(value, valueName, index, null);

        saveIgnoreDuplicateKey(tagEntry);
    }


    private Specification<ResourceTagEntry> getTagEntrySpecification(TagEntryFilters filters, Boolean disabled) {
        Specification<ResourceTagEntry> spec = getCallingTenantSpec();

        if(Utils.isNotEmpty(filters.entryIds()))
            spec = spec.and(getIdSpec(filters.entryIds()));

        if(Utils.isNotEmpty(filters.entryKeys()))
            spec = spec.and(getKeySpec(filters.entryKeys()));

        if(Utils.isNotEmpty(filters.resourceCategories()))
            spec = spec.and(getResourceCategorySpec(filters.resourceCategories()));

        if(Utils.isNotBlank(filters.search()))
            spec = spec.and(getSearchSpec(filters.search()));

        if(disabled != null)
            spec = spec.and(getDisabledSpec(disabled));

        if(filters.requiredWhenCreating() != null)
            spec = spec.and(getRequiredWhenCreatingSpec(filters.requiredWhenCreating()));

        if(filters.requiredWhenFiltering() != null)
            spec = spec.and(getRequiredWhenFilteringSpec(filters.requiredWhenFiltering()));

        if(filters.userGroupTaggable() != null)
            spec = spec.and(getUserGroupTaggableSpec(filters.userGroupTaggable()));

        return spec;
    }

    private Specification<ResourceTagEntry> getKeySpec(List<String> entryKeys) {
        return (root, query, criteriaBuilder) -> root.get("tagKey").in(entryKeys);
    }

    private Specification<ResourceTagEntry> getDisabledSpec(Boolean disabled) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("disabled"), disabled);
    }


    private Specification<ResourceTagEntry> getUserGroupTaggableSpec(Boolean userGroupTaggable) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                root.get("userGroupTaggable"), userGroupTaggable
        );
    }


    private Specification<ResourceTagEntry> getRequiredWhenFilteringSpec(Boolean requiredWhenFiltering) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                root.get("requiredWhenFiltering"), requiredWhenFiltering
        );
    }

    private Specification<ResourceTagEntry> getRequiredWhenCreatingSpec(Boolean requiredWhenCreating) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                root.get("requiredWhenCreating"), requiredWhenCreating
        );
    }

    private Specification<ResourceTagEntry> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> {
            Predicate p1 = criteriaBuilder.like(root.get("tagKey"), "%" + search + "%");
            Predicate p2 = criteriaBuilder.like(root.get("tagKeyName"), "%" + search + "%");
            return criteriaBuilder.or(p1, p2);
        };
    }

    private Specification<ResourceTagEntry> getResourceCategorySpec(List<String> resourceCategories) {
        return (root, query, criteriaBuilder) -> {
            Predicate p1 = root.get("resourceCategory").in(resourceCategories);
            Predicate p2 = criteriaBuilder.isNull(root.get("resourceCategory"));
            return criteriaBuilder.or(p1, p2);
        };
    }
}
