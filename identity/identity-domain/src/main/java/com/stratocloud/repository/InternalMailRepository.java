package com.stratocloud.repository;

import com.stratocloud.jpa.repository.AuditableRepository;
import com.stratocloud.notification.internal.InternalMail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InternalMailRepository extends AuditableRepository<InternalMail> {
    Page<InternalMail> page(String search, Boolean read, Pageable pageable);
}
