package com.stratocloud.repository;

import com.stratocloud.jpa.repository.TenantedRepository;
import com.stratocloud.tag.ResourceTagValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TagValueRepository extends TenantedRepository<ResourceTagValue> {
    Page<ResourceTagValue> page(String tagEntryKey,
                                String search,
                                List<String> tagValues,
                                Pageable pageable);
}
