package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.TenantedJpaRepository;
import com.stratocloud.rule.Rule;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface RuleJpaRepository extends TenantedJpaRepository<Rule>, JpaSpecificationExecutor<Rule> {
    Optional<Rule> findByTypeAndTenantId(String type, Long tenantId);
}
