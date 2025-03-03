package com.stratocloud.secrets;

import com.stratocloud.jpa.repository.jpa.AuditableJpaRepository;

import java.util.Optional;

public interface SecretJpaRepository extends AuditableJpaRepository<Secret> {
    Optional<Secret> findByEncryptedValue(String encryptedValue);
}
