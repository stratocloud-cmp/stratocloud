package com.stratocloud.secrets;

import com.stratocloud.jpa.repository.AbstractAuditableRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class SecretRepositoryImpl extends AbstractAuditableRepository<Secret, SecretJpaRepository>
        implements SecretRepository {
    public SecretRepositoryImpl(SecretJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Secret> findByEncryptedValue(String encryptedValue) {
        return jpaRepository.findByEncryptedValue(encryptedValue);
    }
}
