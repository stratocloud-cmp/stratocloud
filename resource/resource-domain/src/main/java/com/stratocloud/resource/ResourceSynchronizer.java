package com.stratocloud.resource;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.repository.ExternalAccountRepository;
import com.stratocloud.repository.ResourceRepository;
import com.stratocloud.repository.TagEntryRepository;
import com.stratocloud.tag.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ResourceSynchronizer {
    private final ResourceRepository resourceRepository;

    private final ExternalAccountRepository accountRepository;

    private final TagEntryRepository tagEntryRepository;

    public ResourceSynchronizer(ResourceRepository resourceRepository,
                                ExternalAccountRepository accountRepository,
                                TagEntryRepository tagEntryRepository) {
        this.resourceRepository = resourceRepository;
        this.accountRepository = accountRepository;
        this.tagEntryRepository = tagEntryRepository;
    }

    @Transactional
    public void synchronize(Long resourceId) {
        Resource resource = resourceRepository.findResource(resourceId);

        synchronizeSelf(resource);

        synchronizeCost(resource);

        ExternalAccount externalAccount = accountRepository.findExternalAccount(resource.getAccountId());
        ResourceHandler resourceHandler = resource.getResourceHandler();

        if(resource.getSyncState() != ResourceSyncState.OK){
            resourceRepository.saveWithSystemSession(resource);
            return;
        }

        ExternalResource externalResource = resourceHandler.describeExternalResource(
                externalAccount, resource.getExternalId()
        ).orElseThrow(ExternalResourceNotFoundException::new);

        synchronizeManagedRequirements(resource, externalAccount, externalResource);
        synchronizeNewRequirements(resource, externalAccount, externalResource);
        synchronizeTags(resource, externalAccount, externalResource);

        resourceRepository.saveWithSystemSession(resource);
    }

    private void synchronizeCost(Resource resource) {
        resource.synchronizeCost();
    }


    private void synchronizeSelf(Resource resource) {
        resource.synchronize();
    }


    private void synchronizeTags(Resource resource,
                                 ExternalAccount externalAccount,
                                 ExternalResource externalResource) {
        ResourceHandler resourceHandler = resource.getResourceHandler();

        List<Tag> tags = resourceHandler.describeExternalTags(externalAccount, externalResource);

        tags.forEach(tag -> resource.updateTag(tag.entry().key(), tag.entry().name(), tag.value(), tag.valueName()));
        tags.forEach(tag -> {
            try {
                tagEntryRepository.ensureTagValue(
                        resource.getCategory(),
                        tag.entry().key(),
                        tag.entry().name(),
                        tag.value(),
                        tag.valueName(),
                        tag.index()
                );
            }catch (Exception e){
                log.warn("Failed to ensure tag {}.", tag);
            }
        });
    }


    private void synchronizeManagedRequirements(Resource resource,
                                                ExternalAccount externalAccount,
                                                ExternalResource externalResource) {
        List<ExternalRequirement> externalRequirements
                = resource.getResourceHandler().describeExternalRequirements(externalAccount, externalResource);

        for (Relationship requirement : resource.getRequirements()) {
            if(requirement.getState() == RelationshipState.DISCONNECTED)
                continue;

            boolean existed = externalRequirements.stream().anyMatch(
                    er -> Objects.equals(requirement.getType(), er.relationshipTypeId()) &&
                            requirement.getTarget().isSameResource(er.target())
            );

            if(existed) {
                requirement.onConnected();
            } else {
                if(requirement.getHandler().disconnectOnLost()){
                    requirement.onDisconnected();
                }else if(requirement.getState() != RelationshipState.LOST) {
                    requirement.onLost();
                    log.warn("Cannot find relationship {} from {} to {} anymore, changing its state to lost.",
                            requirement.getType(), resource.getName(), requirement.getTarget().getName());
                }
            }
        }

        markDuplicatedRequirementsDisconnected(resource);
    }

    private static void markDuplicatedRequirementsDisconnected(Resource resource) {
        Map<Long, List<Relationship>> connectedTargetsMap = resource.getRequirements().stream().filter(
                r -> r.getState() == RelationshipState.CONNECTED
        ).collect(
                Collectors.groupingBy(r -> r.getTarget().getId())
        );

        for (Map.Entry<Long, List<Relationship>> entry : connectedTargetsMap.entrySet()) {
            Optional<Relationship> latest = entry.getValue().stream().max(
                    Comparator.comparingLong(Relationship::getId)
            );

            if(latest.isEmpty())
                continue;

            for (Relationship relationship : entry.getValue()) {
                if(relationship != latest.get()) {
                    log.warn("Duplicated requirement detected, disconnecting...");
                    relationship.onDisconnected();
                }
            }
        }
    }


    private void synchronizeNewRequirements(Resource resource,
                                            ExternalAccount externalAccount,
                                            ExternalResource externalResource) {
        List<ExternalRequirement> externalRequirements
                = resource.getResourceHandler().describeExternalRequirements(externalAccount, externalResource);

        for (ExternalRequirement externalRequirement : externalRequirements) {
            if(resource.hasRequirement(externalRequirement))
                continue;
            Optional<Resource> target = resourceRepository.findByExternalResource(externalRequirement.target());
            if(target.isEmpty()) {
                log.warn("Relationship {}'s target {} not managed yet.",
                        externalRequirement.relationshipTypeId(), externalRequirement.target().name());
                continue;
            }
            Relationship relationship = resource.addRequirement(
                    target.get(),
                    externalRequirement.relationshipTypeId(),
                    externalRequirement.properties()
            );
            relationship.onConnected();
        }
    }
}
