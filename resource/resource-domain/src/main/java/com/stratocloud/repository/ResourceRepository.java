package com.stratocloud.repository;

import com.stratocloud.jpa.repository.ControllableRepository;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceFilters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ResourceRepository extends ControllableRepository<Resource> {
    Resource findResource(Long resourceId);

    Resource lockResource(Long resourceId);

    List<Resource> findAllByFilters(ResourceFilters filters);

    Page<Resource> page(ResourceFilters filters, Pageable pageable);


    Page<Resource> pageUnclaimed(String category, String search, List<Long> resourceIds, Pageable pageable);

    long countByFilters(ResourceFilters resourceFilters);

    boolean existsByName(String name);

    boolean existsByExternalResource(ExternalResource externalResource);

    Optional<Resource> findByExternalResource(ExternalResource externalResource);

    long countByName(String name);


}
