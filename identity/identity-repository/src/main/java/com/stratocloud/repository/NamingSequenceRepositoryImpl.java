package com.stratocloud.repository;

import com.stratocloud.jpa.repository.AbstractTenantedRepository;
import com.stratocloud.rule.NamingSequence;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class NamingSequenceRepositoryImpl
        extends AbstractTenantedRepository<NamingSequence, NamingSequenceJpaRepository>
        implements NamingSequenceRepository {

    public NamingSequenceRepositoryImpl(NamingSequenceJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NamingSequence> findNamingSequence(String type, String prefix) {
        return jpaRepository.findByRuleTypeAndPrefix(type, prefix);
    }
}
