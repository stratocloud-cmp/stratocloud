package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.TenantedJpaRepository;
import com.stratocloud.tag.ResourceTagValue;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TagValueJpaRepository extends TenantedJpaRepository<ResourceTagValue>,
        JpaSpecificationExecutor<ResourceTagValue> {
}
