package com.stratocloud.repository;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.account.ExternalAccountFilters;
import com.stratocloud.jpa.repository.TenantedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ExternalAccountRepository extends TenantedRepository<ExternalAccount> {
    ExternalAccount findExternalAccount(Long externalAccountId);

    List<ExternalAccount> findByDisabled(boolean disabled);

    Page<ExternalAccount> page(ExternalAccountFilters filters, Pageable pageable);
}
