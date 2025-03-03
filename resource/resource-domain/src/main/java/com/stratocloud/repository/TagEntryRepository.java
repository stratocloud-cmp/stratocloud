package com.stratocloud.repository;

import com.stratocloud.jpa.repository.TenantedRepository;
import com.stratocloud.tag.ResourceTagEntry;
import com.stratocloud.tag.TagEntryFilters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface TagEntryRepository extends TenantedRepository<ResourceTagEntry> {
    ResourceTagEntry findTagEntry(Long tagEntryId);

    Optional<ResourceTagEntry> findByTagKey(String tagKey);

    Page<ResourceTagEntry> page(TagEntryFilters filters, Boolean disabled, Pageable pageable);

    void ensureTagValue(String category, String key, String keyName, String value, String valueName, int index);
}
