package com.stratocloud.resource;

import com.stratocloud.external.resource.UserGatewayService;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.relationship.*;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.cmd.create.NestedResourceTag;
import com.stratocloud.resource.query.NestedRelationshipResponse;
import com.stratocloud.resource.query.NestedResourceResponse;
import com.stratocloud.resource.query.NestedResourceUsage;
import com.stratocloud.resource.query.NestedRuntimeProperty;
import com.stratocloud.resource.query.metadata.*;
import com.stratocloud.utils.Utils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class ResourceAssembler {

    private final UserGatewayService userGatewayService;

    public ResourceAssembler(UserGatewayService userGatewayService) {
        this.userGatewayService = userGatewayService;
    }

    public NestedResourceResponse toResourceResponse(Resource resource) {
        NestedResourceResponse response = new NestedResourceResponse();

        EntityUtil.copyBasicFields(resource, response);

        response.setProviderId(resource.getProviderId());
        response.setProviderName(resource.getResourceHandler().getProvider().getName());
        response.setAccountId(resource.getAccountId());
        response.setCategory(resource.getCategory());
        response.setCategoryName(resource.getResourceHandler().getResourceCategory().name());
        response.setType(resource.getType());
        response.setTypeName(resource.getResourceHandler().getResourceTypeName());
        response.setExternalId(resource.getExternalId());
        response.setName(resource.getName());
        response.setDescription(resource.getDescription());
        response.setState(resource.getState());
        response.setSyncState(resource.getSyncState());
        response.setRuntimeProperties(toNestedRuntimeProperties(resource.getRuntimeProperties()));
        response.setAllocatedUsages(toAllocatedUsages(resource.getAllocatedUsages()));
        response.setPreAllocatedUsages(toPreAllocatedUsages(resource.getPreAllocatedUsages()));
        response.setTags(toNestedResourceTags(resource.getTags()));
        response.setRecycled(resource.getRecycled());
        response.setRecycledTime(resource.getRecycledTime());

        response.setCostDescription(resource.getCost().toDescription());
        response.setMonthlyCostDescription(resource.getCost().toMonthlyCostDescription());

        return response;
    }

    private List<NestedResourceTag> toNestedResourceTags(List<ResourceTag> tags) {
        List<NestedResourceTag> result = new ArrayList<>();
        if(Utils.isEmpty(tags))
            return result;
        for (ResourceTag tag : tags) {
            NestedResourceTag nestedResourceTag = new NestedResourceTag();
            nestedResourceTag.setTagKey(tag.getTagKey());
            nestedResourceTag.setTagKeyName(tag.getTagKeyName());
            nestedResourceTag.setTagValue(tag.getTagValue());
            nestedResourceTag.setTagValueName(tag.getTagValueName());

            result.add(nestedResourceTag);
        }
        return result;
    }

    private List<NestedResourceUsage> toPreAllocatedUsages(List<PreAllocatedResourceUsage> preAllocatedUsages) {
        List<NestedResourceUsage> result = new ArrayList<>();

        if(Utils.isEmpty(preAllocatedUsages))
            return result;

        for (PreAllocatedResourceUsage preAllocatedResourceUsage : preAllocatedUsages) {
            ResourceUsage resourceUsage = preAllocatedResourceUsage.getResourceUsage();
            Resource resource = preAllocatedResourceUsage.getResource();

            NestedResourceUsage nestedResourceUsage = toNestedResourceUsage(resourceUsage, resource);

            result.add(nestedResourceUsage);
        }

        return result;
    }

    private List<NestedResourceUsage> toAllocatedUsages(List<AllocatedResourceUsage> allocatedUsages) {
        List<NestedResourceUsage> result = new ArrayList<>();

        if(Utils.isEmpty(allocatedUsages))
            return result;

        for (AllocatedResourceUsage allocatedUsage : allocatedUsages) {
            ResourceUsage resourceUsage = allocatedUsage.getResourceUsage();
            Resource resource = allocatedUsage.getResource();

            NestedResourceUsage nestedResourceUsage = toNestedResourceUsage(resourceUsage, resource);

            result.add(nestedResourceUsage);
        }

        return result;
    }

    private static NestedResourceUsage toNestedResourceUsage(ResourceUsage resourceUsage, Resource resource) {
        ResourceHandler resourceHandler = resource.getResourceHandler();
        ResourceUsageType usageType = resourceHandler.getUsageType(
                resourceUsage.getUsageType()
        );
        NestedResourceUsage nestedResourceUsage = new NestedResourceUsage();
        nestedResourceUsage.setUsageType(usageType.type());
        nestedResourceUsage.setUsageTypeName(usageType.name());
        nestedResourceUsage.setUsageValue(resourceUsage.getUsageValue());
        return nestedResourceUsage;
    }

    private List<NestedRuntimeProperty> toNestedRuntimeProperties(List<RuntimeProperty> runtimeProperties) {
        List<NestedRuntimeProperty> result = new ArrayList<>();
        if(Utils.isEmpty(runtimeProperties))
            return result;
        for (RuntimeProperty runtimeProperty : runtimeProperties) {
            NestedRuntimeProperty nestedRuntimeProperty = new NestedRuntimeProperty();
            nestedRuntimeProperty.setKey(runtimeProperty.getKey());
            nestedRuntimeProperty.setKeyName(runtimeProperty.getKeyName());
            nestedRuntimeProperty.setValue(runtimeProperty.getValue());
            nestedRuntimeProperty.setValueName(runtimeProperty.getValueName());
            nestedRuntimeProperty.setDisplayable(runtimeProperty.getDisplayable());
            nestedRuntimeProperty.setSearchable(runtimeProperty.getSearchable());
            nestedRuntimeProperty.setDisplayInList(runtimeProperty.getDisplayInList());
            result.add(nestedRuntimeProperty);
        }
        return result;
    }


    public NestedRelationshipResponse toNestedRelationshipResponse(Relationship relationship) {
        NestedRelationshipResponse response = new NestedRelationshipResponse();

        EntityUtil.copyBasicFields(relationship, response);

        response.setState(relationship.getState());
        response.setTarget(toResourceResponse(relationship.getTarget()));
        response.setSource(toResourceResponse(relationship.getSource()));

        return response;
    }

    public NestedResourceType toNestedResourceType(ResourceHandler resourceHandler) {

        NestedResourceTypeSpec spec = toNestedResourceTypeSpec(resourceHandler);

        List<NestedResourceTypeRequirement> requirements = resourceHandler.getRequirements().stream().sorted(
                RelationshipHandler::compareRequirement
        ).map(
                this::toNestedResourceTypeRequirement
        ).toList();

        List<NestedResourceTypeCapability> capabilities = resourceHandler.getCapabilities().stream().sorted(
                RelationshipHandler::compareCapability
        ).map(
                this::toNestedResourceTypeCapability
        ).toList();

        NestedResourceType resourceType = new NestedResourceType();
        resourceType.setSpec(spec);
        resourceType.setRequirements(requirements);
        resourceType.setCapabilities(capabilities);

        return resourceType;
    }


    private NestedResourceTypeCapability toNestedResourceTypeCapability(RelationshipHandler relationshipHandler) {
        ResourceHandler source = relationshipHandler.getSource();

        NestedResourceTypeSpec sourceSpec = toNestedResourceTypeSpec(source);

        NestedRelationshipSpec relationshipSpec = toNestedRelationshipSpec(relationshipHandler);

        List<NestedResourceTypeRequirement> requirements = source.getRequirements().stream().filter(
                r -> isVisibleCapabilityRequirement(relationshipHandler, r)
        ).map(
                this::toNestedResourceTypeRequirement
        ).toList();

        List<NestedResourceTypeCapability> capabilities = source.getCapabilities().stream().map(
                this::toNestedResourceTypeCapability
        ).toList();

        NestedResourceTypeCapability capability = new NestedResourceTypeCapability();
        capability.setSourceSpec(sourceSpec);
        capability.setRelationshipSpec(relationshipSpec);
        capability.setSourceRequirements(requirements);
        capability.setSourceCapabilities(capabilities);

        return capability;
    }

    private static boolean isVisibleCapabilityRequirement(RelationshipHandler mainRequirement,
                                                          RelationshipHandler checkingRequirement) {
        if(Objects.equals(checkingRequirement.getRelationshipTypeId(), mainRequirement.getRelationshipTypeId()))
            return false;

        boolean isMainRequirementPrimaryCapability = mainRequirement instanceof PrimaryCapabilityHandler;

        if(!isMainRequirementPrimaryCapability)
            return true;

        ResourceHandler mainRequirementTarget = mainRequirement.getTarget();
        ResourceHandler checkingRequirementTarget = checkingRequirement.getTarget();
        return !mainRequirementTarget.getResourceCategory().equals(checkingRequirementTarget.getResourceCategory());
    }

    public NestedRelationshipSpec toNestedRelationshipSpec(RelationshipHandler relationshipHandler) {
        NestedRelationshipSpec nestedRelationshipSpec = new NestedRelationshipSpec();

        nestedRelationshipSpec.setRelationshipTypeId(relationshipHandler.getRelationshipTypeId());
        nestedRelationshipSpec.setRelationshipTypeName(relationshipHandler.getRelationshipTypeName());
        nestedRelationshipSpec.setRequirementName(relationshipHandler.getRequirementName());
        nestedRelationshipSpec.setCapabilityName(relationshipHandler.getCapabilityName());
        nestedRelationshipSpec.setConnectActionName(relationshipHandler.getConnectActionName());
        nestedRelationshipSpec.setDisconnectActionName(relationshipHandler.getDisconnectActionName());

        nestedRelationshipSpec.setSourceResourceTypeId(relationshipHandler.getSource().getResourceTypeId());
        nestedRelationshipSpec.setTargetResourceTypeId(relationshipHandler.getTarget().getResourceTypeId());

        nestedRelationshipSpec.setExclusiveRequirement(relationshipHandler instanceof ExclusiveRequirementHandler);
        nestedRelationshipSpec.setEssentialRequirement(relationshipHandler instanceof EssentialRequirementHandler);
        nestedRelationshipSpec.setChangeableEssential(relationshipHandler instanceof ChangeableEssentialHandler);
        nestedRelationshipSpec.setPrimaryCapability(relationshipHandler instanceof PrimaryCapabilityHandler);
        nestedRelationshipSpec.setEssentialPrimaryCapability(
                relationshipHandler instanceof EssentialPrimaryCapabilityHandler
        );


        nestedRelationshipSpec.setAllowedSourceStates(relationshipHandler.getAllowedSourceStates());
        nestedRelationshipSpec.setAllowedTargetStates(relationshipHandler.getAllowedTargetStates());

        nestedRelationshipSpec.setVisibleInTarget(relationshipHandler.visibleInTarget());
        nestedRelationshipSpec.setIsolatedTargetContext(relationshipHandler.isolatedTargetContext());

        return nestedRelationshipSpec;
    }

    private NestedResourceTypeRequirement toNestedResourceTypeRequirement(RelationshipHandler relationshipHandler) {
        ResourceHandler target = relationshipHandler.getTarget();

        NestedResourceTypeSpec targetSpec = toNestedResourceTypeSpec(target);

        NestedRelationshipSpec relationshipSpec = toNestedRelationshipSpec(relationshipHandler);

        List<NestedResourceTypeRequirement> requirements = target.getRequirements().stream().map(
                this::toNestedResourceTypeRequirement
        ).toList();

        NestedResourceTypeRequirement requirement = new NestedResourceTypeRequirement();
        requirement.setTargetSpec(targetSpec);
        requirement.setRelationshipSpec(relationshipSpec);
        requirement.setTargetRequirements(requirements);

        return requirement;
    }

    private NestedResourceTypeSpec toNestedResourceTypeSpec(ResourceHandler resourceHandler) {
        NestedResourceTypeSpec nestedResourceTypeSpec = new NestedResourceTypeSpec();
        nestedResourceTypeSpec.setResourceCategoryId(resourceHandler.getResourceCategory().id());
        nestedResourceTypeSpec.setResourceCategoryName(resourceHandler.getResourceCategory().name());
        nestedResourceTypeSpec.setProviderId(resourceHandler.getProvider().getId());
        nestedResourceTypeSpec.setProviderName(resourceHandler.getProvider().getName());
        nestedResourceTypeSpec.setResourceTypeId(resourceHandler.getResourceTypeId());
        nestedResourceTypeSpec.setResourceTypeName(resourceHandler.getResourceTypeName());
        nestedResourceTypeSpec.setManageable(resourceHandler.isManageable());
        nestedResourceTypeSpec.setInfrastructure(resourceHandler.isInfrastructure());
        nestedResourceTypeSpec.setSharedRequirementTarget(resourceHandler.isSharedRequirementTarget());
        nestedResourceTypeSpec.setBuildable(
                resourceHandler.getActionHandler(ResourceActions.BUILD_RESOURCE).isPresent()
        );
        nestedResourceTypeSpec.setDestroyable(
                resourceHandler.getActionHandler(ResourceActions.DESTROY_RESOURCE).isPresent()
        );
        nestedResourceTypeSpec.setCanAttachIpPool(resourceHandler.canAttachIpPool());
        return nestedResourceTypeSpec;
    }

    public NestedResourceCategory toNestedResourceCategory(ResourceCategory resourceCategory) {
        NestedResourceCategory category = new NestedResourceCategory();
        category.setCategoryId(resourceCategory.id());
        category.setCategoryName(resourceCategory.name());

        category.setGroupId(resourceCategory.group().id());
        category.setGroupName(resourceCategory.group().name());

        return category;
    }

    public NestedProvider toProviderResponse(Provider provider) {
        NestedProvider nestedProvider = new NestedProvider();
        nestedProvider.setId(provider.getId());
        nestedProvider.setName(provider.getName());

        var accountPropertiesClass = provider.getExternalAccountPropertiesClass();

        DynamicFormMetaData metaData = DynamicFormHelper.generateMetaData(accountPropertiesClass);
        nestedProvider.setAccountFormMetaData(metaData);

        return nestedProvider;
    }

    public Page<NestedResourceResponse> convertPage(Page<Resource> page) {
        Page<NestedResourceResponse> responsePage = page.map(this::toResourceResponse);
        EntityUtil.fillOwnerInfo(responsePage, userGatewayService);
        return responsePage;
    }
}
