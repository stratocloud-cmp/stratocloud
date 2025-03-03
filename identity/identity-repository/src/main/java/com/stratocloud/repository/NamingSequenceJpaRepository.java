package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.TenantedJpaRepository;
import com.stratocloud.rule.NamingSequence;

import java.util.Optional;

public interface NamingSequenceJpaRepository extends TenantedJpaRepository<NamingSequence> {
    Optional<NamingSequence> findByRuleTypeAndPrefix(String type, String prefix);
}
