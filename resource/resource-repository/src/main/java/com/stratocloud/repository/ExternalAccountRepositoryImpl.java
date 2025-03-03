package com.stratocloud.repository;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.account.ExternalAccountFilters;
import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.jpa.repository.AbstractTenantedRepository;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.ProviderRegistry;
import com.stratocloud.utils.Utils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class ExternalAccountRepositoryImpl
        extends AbstractTenantedRepository<ExternalAccount, ExternalAccountJpaRepository>
        implements ExternalAccountRepository {

    public ExternalAccountRepositoryImpl(ExternalAccountJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public ExternalAccount findExternalAccount(Long externalAccountId) {
        return jpaRepository.findById(externalAccountId).orElseThrow(
                () -> new EntityNotFoundException("External account not found.")
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExternalAccount> findByDisabled(boolean disabled) {
        return jpaRepository.findByDisabled(disabled);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExternalAccount> page(ExternalAccountFilters filters, Pageable pageable) {
        Specification<ExternalAccount> spec = getExternalAccountSpecification(filters);

        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<ExternalAccount> getExternalAccountSpecification(ExternalAccountFilters filters) {
        Specification<ExternalAccount> spec = getCallingTenantSpec();

        if(Utils.isNotEmpty(filters.accountIds()))
            spec = spec.and(getIdSpec(filters.accountIds()));

        if(Utils.isNotBlank(filters.resourceCategory()))
            spec = spec.and(getResourceCategorySpec(filters.resourceCategory()));

        if(Utils.isNotEmpty(filters.providerIds()))
            spec = spec.and(getProviderSpec(filters.providerIds()));

        if(Utils.isNotBlank(filters.search()))
            spec = spec.and(getSearchSpec(filters.search()));

        if(filters.disabled() != null)
            spec = spec.and(getDisabledSpec(filters.disabled()));

        return spec;
    }

    private Specification<ExternalAccount> getResourceCategorySpec(String resourceCategory) {
        Set<String> providerIds = ProviderRegistry.getProviders().stream().filter(
                provider -> provider.getResourceHandlerByCategory(resourceCategory).isPresent()
        ).map(Provider::getId).collect(Collectors.toSet());
        return getProviderSpec(providerIds);
    }

    private Specification<ExternalAccount> getDisabledSpec(Boolean disabled) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("disabled"), disabled);
    }

    private Specification<ExternalAccount> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("name"), "%"+search+"%");
    }

    private Specification<ExternalAccount> getProviderSpec(Collection<String> providerIds) {
        return (root, query, criteriaBuilder) -> root.get("providerId").in(providerIds);
    }
}
