package com.stratocloud.repository;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.jpa.repository.jpa.TenantedJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ExternalAccountJpaRepository
        extends TenantedJpaRepository<ExternalAccount>, JpaSpecificationExecutor<ExternalAccount> {
    List<ExternalAccount> findByDisabled(boolean disabled);
}
