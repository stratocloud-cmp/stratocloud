package com.stratocloud.jpa.repository.jpa;

import com.stratocloud.jpa.entities.Auditable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface AuditableJpaRepository<E extends Auditable> extends JpaRepository<E, Long> {
}
