package com.stratocloud.resource;

import com.stratocloud.external.resource.RuleGatewayService;
import com.stratocloud.provider.ProviderRegistry;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.repository.ResourceRepository;
import com.stratocloud.repository.TagEntryRepository;
import com.stratocloud.resource.cmd.create.*;
import com.stratocloud.resource.naming.ResourceRuleTypes;
import com.stratocloud.tag.ResourceTagEntry;
import com.stratocloud.utils.RandomUtil;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class ResourceFactory {
    private final ResourceRepository resourceRepository;

    private final RuleGatewayService ruleGatewayService;

    private final TagEntryRepository tagEntryRepository;

    public ResourceFactory(ResourceRepository resourceRepository,
                           RuleGatewayService ruleGatewayService,
                           TagEntryRepository tagEntryRepository) {
        this.resourceRepository = resourceRepository;
        this.ruleGatewayService = ruleGatewayService;
        this.tagEntryRepository = tagEntryRepository;
    }


    public Resource createResource(CreateResourcesCmd cmd) {
        return createResource(cmd, true);
    }

    public Resource createResource(CreateResourcesCmd cmd, boolean ensureResourceNames) {
        Long tenantId = cmd.getTenantId();
        Long ownerId = cmd.getOwnerId();
        NestedNewResource nestedNewResource = cmd.getResource();


        List<NestedNewRequirement> requirements = cmd.getRequirements();
        List<NestedNewCapability> capabilities = cmd.getCapabilities();

        Resource resource = createResource(tenantId, ownerId, nestedNewResource, ensureResourceNames);

        createRequirements(requirements, resource);

        createCapabilities(tenantId, ownerId, capabilities, resource, ensureResourceNames);

        return resource;
    }

    private void createCapabilities(Long tenantId,
                                    Long ownerId,
                                    List<NestedNewCapability> capabilities,
                                    Resource resource,
                                    boolean ensureResourceNames) {
        if(Utils.isEmpty(capabilities))
            return;

        for (NestedNewCapability capability : capabilities) {
            NestedNewResource nestedNewResource = capability.getResource();
            Resource source = createResource(tenantId, ownerId, nestedNewResource, ensureResourceNames);

            resource.addCapability(source, capability.getRelationshipTypeId(), capability.getRelationshipProperties());

            inheritTags(resource, source);

            createRequirements(capability.getRequirements(), source);
            createCapabilities(tenantId, ownerId, capability.getCapabilities(), source, ensureResourceNames);
        }
    }

    private void inheritTags(Resource parent, Resource child) {
        if(Utils.isNotEmpty(parent.getTags()) && Utils.isEmpty(child.getTags())){
            for (ResourceTag tag : parent.getTags()) {
                Optional<ResourceTagEntry> tagEntry = tagEntryRepository.findByTagKey(tag.getTagKey());

                if(tagEntry.isEmpty())
                    continue;

                Boolean requiredWhenCreating = tagEntry.get().getRequiredWhenCreating();
                boolean noLimitedCategory = Utils.isBlank(tagEntry.get().getResourceCategory());

                if(requiredWhenCreating && noLimitedCategory){
                    ResourceTag copy = ResourceTag.copyOf(tag);
                    child.addTag(copy);
                }
            }
        }
    }

    private void createRequirements(List<NestedNewRequirement> requirements, Resource resource) {
        if(Utils.isEmpty(requirements))
            return;

        for (NestedNewRequirement requirement : requirements) {
            Long targetResourceId = requirement.getTargetResourceId();
            String relationshipTypeId = requirement.getRelationshipTypeId();
            Map<String, Object> relationshipInputs = requirement.getRelationshipInputs();

            Resource target = resourceRepository.findResource(targetResourceId);

            resource.addRequirement(target, relationshipTypeId, relationshipInputs);
        }
    }

    public Resource createResource(Long tenantId,
                                   Long ownerId,
                                   NestedNewResource nestedNewResource,
                                   boolean ensureResourceNames){
        ResourceHandler resourceHandler = ProviderRegistry.getResourceHandler(nestedNewResource.getResourceTypeId());



        String resourceName = null;

        if(ensureResourceNames){
            resourceName = ensureResourceName(
                    tenantId, ownerId, nestedNewResource, resourceHandler.getResourceCategory()
            );
        }


        List<NestedResourceTag> newResourceTags = nestedNewResource.getTags();
        List<ResourceTag> tags = convertTags(newResourceTags);

        return new Resource(
                tenantId, ownerId,
                resourceHandler.getProvider().getId(),
                nestedNewResource.getExternalAccountId(),
                resourceHandler.getResourceCategory().id(),
                resourceHandler.getResourceTypeId(),
                resourceName,
                nestedNewResource.getDescription(),
                nestedNewResource.getProperties(),
                tags
        );
    }

    private String ensureResourceName(Long tenantId,
                                      Long ownerId,
                                      NestedNewResource nestedNewResource,
                                      ResourceCategory resourceCategory) {
        String name;

        if(Utils.isNotBlank(nestedNewResource.getResourceName()))
            name = nestedNewResource.getResourceName();
        else
            name = generateResourceName(tenantId, ownerId, nestedNewResource, resourceCategory);

        long count = 1;

        boolean existed = resourceRepository.existsByName(name);

        String tempName = name;

        while (existed){
            if(count > 30){
                tempName = name + "-" + RandomUtil.generateRandomString(3);
                log.warn("Using random resource name: {}", tempName);
                break;
            }
            tempName = name + "-" + count;
            existed = resourceRepository.existsByName(tempName);
            count++;
        }

        nestedNewResource.setResourceName(tempName);
        return tempName;
    }

    private String generateResourceName(Long tenantId, Long ownerId, NestedNewResource nestedNewResource, ResourceCategory resourceCategory) {
        Map<String, Object> args = new HashMap<>();
        args.put("tenantId", tenantId);
        args.put("ownerId", ownerId);
        args.put("resource", nestedNewResource);
        args.put("resourceCategoryAbbr", resourceCategory.abbr());
        String name = ruleGatewayService.executeNamingRule(ResourceRuleTypes.RESOURCE_NAMING_RULE, args);


        while (resourceRepository.existsByName(name)){
            log.warn("Resource name {} already exists, regenerating...", name);
            String nextName = ruleGatewayService.executeNamingRule(ResourceRuleTypes.RESOURCE_NAMING_RULE, args);
            if(Objects.equals(nextName, name)) {
                log.warn("Static resource naming policy detected.");
                return name;
            }
            name = nextName;
        }
        return name;
    }

    private static List<ResourceTag> convertTags(List<NestedResourceTag> newResourceTags) {
        return Utils.isEmpty(newResourceTags) ? new ArrayList<>() : newResourceTags.stream().map(
                t->new ResourceTag(t.getTagKey(), t.getTagKeyName(), t.getTagValue(), t.getTagValueName())
        ).toList();
    }
}
