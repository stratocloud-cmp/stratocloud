package com.stratocloud.resource;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.repository.ExternalAccountRepository;
import com.stratocloud.repository.ResourceRepository;
import com.stratocloud.resource.license.LicensedResourcesLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Component
public class ResourceManagementService {

    private final ExternalAccountRepository accountRepository;

    private final ResourceRepository resourceRepository;


    private final ResourceSynchronizer synchronizer;

    private final LicensedResourcesLimiter licensedResourcesLimiter;

    public ResourceManagementService(ExternalAccountRepository accountRepository,
                                     ResourceRepository resourceRepository,
                                     ResourceSynchronizer synchronizer,
                                     LicensedResourcesLimiter licensedResourcesLimiter) {
        this.accountRepository = accountRepository;
        this.resourceRepository = resourceRepository;
        this.synchronizer = synchronizer;
        this.licensedResourcesLimiter = licensedResourcesLimiter;
    }

    @Transactional
    public void manageExternalResource(Long ownerId, ExternalResource externalResource){
        log.info("Managing external resource {}.", externalResource.name());

        ExternalAccount account = accountRepository.findExternalAccount(externalResource.accountId());

        Resource resource;
        Optional<Resource> optionalResource = resourceRepository.findByExternalResource(externalResource);

        if(optionalResource.isPresent()){
            log.info("Resource {} is already managed.", externalResource.name());
            resource = optionalResource.get();
        }else {
            log.info("Resource {} is never managed.", externalResource.name());
            resource = Resource.createFromExternalResource(
                    account.getTenantId(), ownerId, externalResource
            );

            licensedResourcesLimiter.validateLimitForCategory(resource.getCategory());
        }

        resource = resourceRepository.saveWithSystemSession(resource);

        synchronizer.synchronize(resource.getId());

        log.info("Resource {} has been managed successfully.", resource.getName());
    }
}
