package com.stratocloud.secrets;

import com.stratocloud.jpa.repository.AuditableRepository;

import java.util.Optional;

public interface SecretRepository extends AuditableRepository<Secret> {
    Optional<Secret> findByEncryptedValue(String encryptedValue);
}
