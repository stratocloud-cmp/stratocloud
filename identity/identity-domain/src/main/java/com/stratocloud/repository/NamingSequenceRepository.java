package com.stratocloud.repository;

import com.stratocloud.jpa.repository.TenantedRepository;
import com.stratocloud.rule.NamingSequence;

import java.util.Optional;

public interface NamingSequenceRepository extends TenantedRepository<NamingSequence> {
    Optional<NamingSequence> findNamingSequence(String type, String prefix);
}
