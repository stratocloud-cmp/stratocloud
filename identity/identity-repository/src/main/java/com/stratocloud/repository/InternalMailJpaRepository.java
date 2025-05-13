package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.AuditableJpaRepository;
import com.stratocloud.notification.internal.InternalMail;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InternalMailJpaRepository
        extends AuditableJpaRepository<InternalMail>, JpaSpecificationExecutor<InternalMail> {
}
