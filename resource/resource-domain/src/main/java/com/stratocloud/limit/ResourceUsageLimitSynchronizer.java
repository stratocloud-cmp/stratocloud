package com.stratocloud.limit;

import com.stratocloud.auth.CallContext;
import com.stratocloud.external.resource.TenantGatewayService;
import com.stratocloud.identity.SimpleTenant;
import com.stratocloud.repository.ResourceRepository;
import com.stratocloud.repository.ResourceUsageLimitRepository;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceFilters;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ResourceUsageLimitSynchronizer {
    private final ResourceUsageLimitRepository repository;

    private final TenantGatewayService tenantGatewayService;

    private final ResourceRepository resourceRepository;

    public ResourceUsageLimitSynchronizer(ResourceUsageLimitRepository repository,
                                          TenantGatewayService tenantGatewayService,
                                          ResourceRepository resourceRepository) {
        this.repository = repository;
        this.tenantGatewayService = tenantGatewayService;
        this.resourceRepository = resourceRepository;
    }

    private List<Long> getSubTenantIds(Long tenantId) {
        List<SimpleTenant> subTenants
                = tenantGatewayService.findSubTenants(tenantId);
        List<Long> tenantIds = new ArrayList<>();
        subTenants.forEach(st -> tenantIds.add(st.id()));
        return tenantIds;
    }

    @Transactional
    public void synchronizeLimit(Long limitId) {
        CallContext current = CallContext.current();
        CallContext.registerSystemSession();

        ResourceUsageLimit limit = repository.findLimit(limitId);

        List<Long> tenantIds = getSubTenantIds(limit.getTenantId());
        tenantIds.add(limit.getTenantId());

        List<Long> ownerIds = limit.getOwnerIds();
        List<String> providerIds = limit.getProviderIds();
        List<Long> accountIds = limit.getAccountIds();
        List<String> resourceCategories = limit.getResourceCategories();
        Map<String, List<String>> tagsMap = limit.getTagsMap();

        ResourceFilters filters = ResourceFilters.builder()
                .tenantIds(tenantIds)
                .ownerIds(ownerIds)
                .providerIds(providerIds)
                .accountIds(accountIds)
                .resourceCategories(resourceCategories)
                .tagsMap(tagsMap)
                .build();


        List<Resource> resources = resourceRepository.findAllByFilters(filters);

        limit.synchronize(resources);

        repository.save(limit);

        CallContext.registerBack(current);
    }
}
