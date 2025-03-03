package com.stratocloud.repository;

import com.stratocloud.jpa.repository.TenantedRepository;
import com.stratocloud.rule.Rule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface RuleRepository extends TenantedRepository<Rule> {
    Optional<Rule> findRuleByType(String type);

    Page<Rule> page(List<Long> tenantIds, List<Long> ruleIds, List<String> ruleTypes, String search, Pageable pageable);

    Rule findRule(Long ruleId);
}
