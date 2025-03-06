package com.stratocloud.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.exceptions.ProviderConnectionException;
import com.stratocloud.job.Task;
import com.stratocloud.job.TaskTargetEntity;
import com.stratocloud.jpa.entities.Controllable;
import com.stratocloud.provider.ProviderRegistry;
import com.stratocloud.provider.relationship.*;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.provider.resource.TransientResourceHandler;
import com.stratocloud.resource.task.ResourceActionTaskHandler;
import com.stratocloud.resource.task.SynchronizeResourceTaskHandler;
import com.stratocloud.utils.Utils;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = {
        @Index(name = "combined_idx", columnList = "category"),
        @Index(name = "combined_idx", columnList = "recycled"),
        @Index(name = "combined_idx", columnList = "tenant_id"),
        @Index(name = "combined_idx", columnList = "public_resource"),
        @Index(name = "combined_idx", columnList = "owner_id"),
})
public class Resource extends Controllable implements TaskTargetEntity {
    @Column(updatable = false, nullable = false)
    private String providerId;

    @Column(updatable = false, nullable = false)
    private Long accountId;

    @Column(updatable = false, nullable = false)
    private String category;

    @Column(updatable = false, nullable = false)
    private String type;

    @Column
    private String externalId;

    @OptimisticLock(excluded = true)
    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @OptimisticLock(excluded = true)
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ResourceState state = ResourceState.NO_STATE;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ResourceSyncState syncState = ResourceSyncState.NO_STATE;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "resource", orphanRemoval = true)
    @BatchSize(size = 50)
    private List<RuntimeProperty> runtimeProperties = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "resource", orphanRemoval = true)
    @BatchSize(size = 50)
    private List<AllocatedResourceUsage> allocatedUsages = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "resource", orphanRemoval = true)
    @BatchSize(size = 50)
    private List<PreAllocatedResourceUsage> preAllocatedUsages = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String, Object> properties = new HashMap<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "resource", orphanRemoval = true)
    @BatchSize(size = 50)
    private List<ResourceTag> tags = new ArrayList<>();

    @Column(nullable = false)
    private Boolean recycled = false;

    @Column
    private LocalDateTime recycledTime;

    @Column(nullable = false)
    private Boolean publicResource = false;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    private double cost = ResourceCost.ZERO.cost();
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    private double costTimeAmount = ResourceCost.ZERO.timeAmount();
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChronoUnit costTimeUnit = ResourceCost.ZERO.timeUnit();


    @Getter(AccessLevel.NONE)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "target", orphanRemoval = true)
    private List<Relationship> capabilities = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "source", orphanRemoval = true)
    private List<Relationship> requirements = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @Transient
    @JsonIgnore
    private Relationship transientRequirement;

    public Resource(Long tenantId, Long ownerId, String providerId, Long accountId, String category, String type,
                    String name, String description, Map<String, Object> properties, List<ResourceTag> tags) {
        setTenantId(tenantId);
        setOwnerId(ownerId);

        this.providerId = providerId;
        this.accountId = accountId;
        this.category = category;
        this.type = type;
        this.name = name;
        this.description = description;
        this.properties = properties;

        addTags(tags);

        this.publicResource = getResourceHandler().isInfrastructure();
    }

    public static Resource createFromExternalResource(Long tenantId,
                                                      Long ownerId,
                                                      ExternalResource externalResource){
        Resource r = new Resource();

        r.setTenantId(tenantId);
        r.setOwnerId(ownerId);

        r.providerId = externalResource.providerId();
        r.accountId = externalResource.accountId();
        r.category = externalResource.category();
        r.type = externalResource.type();
        r.externalId = externalResource.externalId();
        r.name = externalResource.name();

        r.publicResource = r.getResourceHandler().isInfrastructure();

        return r;
    }

    @Override
    public String getEntityDescription() {
        ResourceHandler resourceHandler = getResourceHandler();
        return "%s: %s".formatted(resourceHandler.getResourceTypeName(), name);
    }

    public ResourceHandler getResourceHandler() {
        return ProviderRegistry.getResourceHandler(type);
    }

    public Relationship addRequirement(Resource target,
                                       String relationshipTypeId,
                                       Map<String, Object> relationshipProperties) {
        Relationship requirement = createRelationship(this, target, relationshipTypeId, relationshipProperties);
        this.requirements.add(requirement);
        return requirement;
    }

    public void addCapability(Resource source,
                              String relationshipTypeId,
                              Map<String, Object> relationshipProperties) {
        Relationship requirement = createRelationship(source, this, relationshipTypeId, relationshipProperties);
        this.capabilities.add(requirement);
    }

    private static Relationship createRelationship(Resource source,
                                                   Resource target,
                                                   String relationshipTypeId,
                                                   Map<String, Object> relationshipProperties){
        RelationshipHandler relationshipHandler;
        if(DependsOnRelationshipHandler.TYPE_ID.equals(relationshipTypeId)){
            relationshipHandler = new DependsOnRelationshipHandler(
                    source.getResourceHandler(),
                    target.getResourceHandler()
            );
        }else {
            ResourceHandler sourceResourceHandler = source.getResourceHandler();
            relationshipHandler = sourceResourceHandler.getRequirement(relationshipTypeId);
        }

        Relationship relationship = new Relationship();
        relationship.setType(relationshipHandler.getRelationshipTypeId());
        relationship.setTypeName(relationshipHandler.getRelationshipTypeName());
        relationship.setProperties(relationshipProperties);
        relationship.setTarget(target);
        relationship.setSource(source);
        return relationship;
    }

    public List<Resource> getRequirementTargets(List<Relationship> relationships, String relationshipTypeId){
        Set<RelationshipState> excludedStates = Set.of(
                RelationshipState.DISCONNECTED, RelationshipState.LOST
        );
        return relationships.stream().filter(
                relationship -> !excludedStates.contains(relationship.getState())
        ).filter(
                relationship -> Objects.equals(relationship.getType(), relationshipTypeId)
        ).map(Relationship::getTarget).toList();
    }

    public Optional<Resource> getExclusiveTargetByType(String relationshipTypeId){
        return getRequirementTargets(getExclusiveRequirements(), relationshipTypeId).stream().findAny();
    }

    public Optional<Resource> getEssentialTargetByType(String relationshipTypeId){
        return getRequirementTargets(getEssentialRequirements(), relationshipTypeId).stream().findAny();
    }

    public Optional<Relationship> getRelationshipByTarget(Resource target){
        return requirements.stream().filter(
                rel -> rel.getTarget().isSameResource(target)
        ).findAny();
    }

    private boolean isSameResource(Resource that) {
        if(this == that)
            return true;

        return Objects.equals(this.getId(), that.getId());
    }

    private List<Resource> getRequirementTargets(List<Relationship> relationships, ResourceCategory category){
        Set<RelationshipState> excludedStates = Set.of(
                RelationshipState.DISCONNECTED, RelationshipState.LOST
        );
        return relationships.stream().filter(
                relationship -> !excludedStates.contains(relationship.getState())
        ).map(Relationship::getTarget).filter(resource -> resource.isCategory(category)).toList();
    }

    public List<Resource> getRequirementTargets(ResourceCategory category){
        return getRequirementTargets(getRequirementsIncludingTransient(), category);
    }

    public List<Resource> getCapabilitySources(ResourceCategory category){
        Set<RelationshipState> excludedStates = Set.of(
                RelationshipState.DISCONNECTED, RelationshipState.LOST
        );
        return getCapabilities().stream().filter(
                relationship -> !excludedStates.contains(relationship.getState())
        ).map(Relationship::getSource).filter(resource -> resource.isCategory(category)).toList();
    }

    public List<Relationship> getEssentialRequirements(){
        return getRequirementsIncludingTransient().stream().filter(
                relationship -> relationship.getHandler() instanceof EssentialRequirementHandler
        ).toList();
    }

    public List<Relationship> getExclusiveRequirements(){
        return getRequirementsIncludingTransient().stream().filter(
                relationship -> relationship.getHandler() instanceof ExclusiveRequirementHandler
        ).toList();
    }

    public boolean isPrimaryTo(ResourceCategory category){
        Optional<Relationship> requirement = getRequirementsIncludingTransient().stream().filter(
                relationship -> relationship.getTarget().isCategory(category)
        ).findAny();
        return requirement.isPresent() && requirement.get().getHandler() instanceof PrimaryCapabilityHandler;
    }

    public Optional<Resource> getEssentialTarget(ResourceCategory category){
        List<Resource> targets = getRequirementTargets(getEssentialRequirements(), category);
        if(Utils.isEmpty(targets))
            return Optional.empty();

        return Optional.of(targets.get(0));
    }

    public Optional<Resource> getExclusiveTargetByTargetHandler(Class<? extends ResourceHandler> targetHandlerClass){
        return getExclusiveTargetsByTargetHandler(targetHandlerClass).stream().findAny();
    }

    public List<Resource> getExclusiveTargetsByTargetHandler(Class<? extends ResourceHandler> targetHandlerClass){
        Set<RelationshipState> excludedStates = Set.of(
                RelationshipState.DISCONNECTED, RelationshipState.LOST
        );

        return getExclusiveRequirements().stream().filter(
                rel -> !excludedStates.contains(rel.getState())
        ).filter(
                rel -> targetHandlerClass.isAssignableFrom(rel.getHandler().getTarget().getClass())
        ).map(Relationship::getTarget).toList();
    }

    public Optional<Resource> getExclusiveTarget(ResourceCategory category){
        List<Resource> targets = getRequirementTargets(getExclusiveRequirements(), category);
        if(Utils.isEmpty(targets))
            return Optional.empty();

        return Optional.of(targets.get(0));
    }

    public boolean isCategory(ResourceCategory category) {
        return Objects.equals(category.id(), this.category);
    }

    public void preAllocateUsages(List<ResourceUsage> usages, Long taskId) {
        for (ResourceUsage usage : usages) {
            PreAllocatedResourceUsage preAllocatedResourceUsage = new PreAllocatedResourceUsage(usage, taskId);
            preAllocatedResourceUsage.setResource(this);
            preAllocatedUsages.add(preAllocatedResourceUsage);
        }
    }

    public ResourceActionHandler getActionHandler(String actionId) {
        ResourceHandler resourceHandler = getResourceHandler();
        return resourceHandler.getActionHandler(actionId).orElseThrow(
                ()->new BadCommandException("Cannot run resource action %s on resource type %s.".formatted(
                        actionId, type
                ))
        );
    }

    public ResourceReadActionHandler getReadActionHandler(String actionId) {
        ResourceHandler resourceHandler = getResourceHandler();
        return resourceHandler.getReadActionHandler(actionId).orElseThrow(
                ()->new BadCommandException("Cannot run read action %s on resource type %s.".formatted(
                        actionId, type
                ))
        );
    }


    public Task createBuildTask() {
        return createResourceTask(ResourceActions.BUILD_RESOURCE.id(), properties);
    }


    public Task createResourceTask(String actionId, Map<String, Object> parameters){
        ResourceActionHandler actionHandler = getActionHandler(actionId);
        var predictedUsages = actionHandler.predictUsageChangeAfterAction(this, parameters);
        ResourceTaskInputs resourceTaskInputs = new ResourceTaskInputs(actionHandler.getAction(), parameters);
        Task task = new Task(this, ResourceActionTaskHandler.TASK_TYPE, resourceTaskInputs);
        preAllocateUsages(predictedUsages, task.getId());
        return task;
    }

    public void releasePreAllocatedUsagesByTaskId(Long taskId) {
        preAllocatedUsages.removeIf(u-> Objects.equals(taskId, u.getTaskId()));
    }

    public void validateActionPrecondition(String actionId, Map<String, Object> parameters) {
        getResourceHandler().validateActionPrecondition(this, actionId, parameters);
    }

    public void markRecycled(boolean recyclingCapabilities) {
        this.recycled = true;
        this.recycledTime = LocalDateTime.now();

        if(recyclingCapabilities){
            for (Relationship capability : capabilities) {
                capability.getSource().markRecycled(true);
            }
        }
    }

    public Task createDestroyTask(Map<String, Object> parameters) {
        return createResourceTask(ResourceActions.DESTROY_RESOURCE.id(), parameters);
    }


    public boolean hasTag(String tagKey, String tagValue) {
        return tags.stream().anyMatch(
                t -> Objects.equals(t.getTagKey(), tagKey) && Objects.equals(t.getTagValue(), tagValue)
        );
    }

    public void synchronize() {
        ResourceHandler resourceHandler = getResourceHandler();
        try {
            resourceHandler.synchronize(this);
            this.syncState = ResourceSyncState.OK;
        }catch (ExternalResourceNotFoundException e){
            if(resourceHandler instanceof TransientResourceHandler){
                markRecycled(true);
                onDestroyed();
                this.syncState = ResourceSyncState.NOT_FOUND;
            } else {
                this.syncState = ResourceSyncState.NOT_FOUND;
                log.warn("External resource not found, changing syncState to NOT_FOUND: {}", getName());
            }
        }catch (ProviderConnectionException e){
            this.syncState = ResourceSyncState.CONNECTION_ERROR;
            log.warn("Provider connection error, changing syncState to CONNECTION_ERROR.", e);
        }
    }

    public BigDecimal getTotalUsageByType(String usageType) {
        return getAllocatedUsageByType(usageType).add(getPreAllocatedUsageByType(usageType));
    }

    public BigDecimal getAllocatedUsageByType(String usageType) {
        BigDecimal result = BigDecimal.ZERO;

        Optional<AllocatedResourceUsage> allocatedResourceUsage = allocatedUsages.stream().filter(
                u -> Objects.equals(usageType, u.getResourceUsage().getUsageType())
        ).findAny();

        if(allocatedResourceUsage.isPresent())
            result = result.add(allocatedResourceUsage.get().getResourceUsage().getUsageValue());

        return result;
    }

    public BigDecimal getPreAllocatedUsageByType(String usageType) {
        BigDecimal result = BigDecimal.ZERO;

        Optional<PreAllocatedResourceUsage> preAllocatedResourceUsage = preAllocatedUsages.stream().filter(
                u -> Objects.equals(usageType, u.getResourceUsage().getUsageType())
        ).findAny();

        if(preAllocatedResourceUsage.isPresent())
            result = result.add(preAllocatedResourceUsage.get().getResourceUsage().getUsageValue());

        return result;
    }

    public boolean hasRequirement(ExternalRequirement externalRequirement) {
        return requirements.stream().filter(
                r -> r.getState() != RelationshipState.DISCONNECTED
        ).filter(
                r -> Objects.equals(externalRequirement.relationshipTypeId(), r.getType())
        ).anyMatch(
                r -> r.getTarget().isSameResource(externalRequirement.target())
        );
    }

    public boolean isSameResource(ExternalResource externalResource) {
        return Objects.equals(accountId, externalResource.accountId()) &&
                Objects.equals(type, externalResource.type()) &&
                Objects.equals(externalId, externalResource.externalId());
    }

    public void updateTag(String key, String keyName, String value, String valueName) {
        for (ResourceTag tag : tags)
            if (Objects.equals(tag.getTagKey(), key) && Objects.equals(tag.getTagValue(), value))
                return;

        ResourceTag resourceTag = new ResourceTag(key, keyName, value, valueName);
        addTag(resourceTag);
    }

    public void addTag(ResourceTag resourceTag) {
        resourceTag.setResource(this);
        tags.add(resourceTag);
    }

    public void addTags(List<ResourceTag> resourceTags) {
        resourceTags.forEach(this::addTag);
    }


    public Set<ResourceAction> getAvailableActions(){
        return getResourceHandler().getAvailableActions(this);
    }

    public Set<ResourceAction> getAvailableReadActions(){
        return getResourceHandler().getAvailableReadActions(this);
    }

    public void updateByExternal(ExternalResource externalResource) {
        this.name = externalResource.name();
        this.state = externalResource.state();
    }

    public void updateUsageByType(ResourceUsageType usageType, BigDecimal usageValue) {
        Optional<AllocatedResourceUsage> optional = allocatedUsages.stream().filter(
                u -> Objects.equals(usageType.type(), u.getResourceUsage().getUsageType())
        ).findAny();

        AllocatedResourceUsage allocatedResourceUsage;
        if(optional.isEmpty()){
            allocatedResourceUsage = new AllocatedResourceUsage();
            allocatedResourceUsage.setResource(this);
            allocatedUsages.add(allocatedResourceUsage);
        }else {
            allocatedResourceUsage = optional.get();
        }

        allocatedResourceUsage.setResourceUsage(new ResourceUsage(usageType.type(), usageValue));
    }

    public Task createSynchronizeTask() {
        return new Task(
                this,
                SynchronizeResourceTaskHandler.TASK_TYPE,
                new SynchronizeResourceTaskInputs(getId())
        );
    }

    public void addOrUpdateRuntimeProperty(RuntimeProperty runtimeProperty){
        runtimeProperties.removeIf(rp -> Objects.equals(rp.getKey(), runtimeProperty.getKey()));
        runtimeProperties.add(runtimeProperty);
        runtimeProperty.setResource(this);
    }

    public void restore() {
        this.recycled = false;
        this.recycledTime = null;
    }

    public List<Relationship> getRequirements() {
        return requirements;
    }

    public List<Relationship> getCapabilities() {
        if(Utils.isEmpty(capabilities))
            return new ArrayList<>();

        capabilities.forEach(c -> c.getSource().setTransientRequirement(c));

        return capabilities;
    }

    private Optional<Relationship> getTransientRequirement() {
        if(transientRequirement == null)
            return Optional.empty();

        if(requirements.stream().anyMatch(r -> Objects.equals(r.getId(), transientRequirement.getId())))
            return Optional.empty();

        return Optional.ofNullable(transientRequirement);
    }

    private List<Relationship> getRequirementsIncludingTransient(){
        List<Relationship> result = new ArrayList<>();

        if(Utils.isNotEmpty(requirements))
            result.addAll(requirements);

        getTransientRequirement().ifPresent(result::add);

        return Collections.unmodifiableList(result);
    }


    public Optional<ExternalResource> toExternalResource(){
        if(Utils.isBlank(externalId))
            return Optional.empty();
        ExternalResource externalResource = ExternalResource.builder()
                .providerId(providerId)
                .accountId(accountId)
                .category(category)
                .type(type)
                .externalId(externalId)
                .name(name)
                .state(state)
                .build();
        return Optional.of(externalResource);
    }

    public void onDestroyed(){
        setState(ResourceState.DESTROYED);

        if(Utils.isNotEmpty(requirements))
            requirements.forEach(Relationship::onDisconnected);
    }

    public Optional<Resource> getPrimaryCapability(ResourceCategory category) {
        Set<RelationshipState> excludedStates = Set.of(
                RelationshipState.DISCONNECTED, RelationshipState.LOST
        );

        return capabilities.stream().filter(
                c -> c.getSource().isCategory(category) && c.isPrimaryCapability()
        ).filter(
                c -> !excludedStates.contains(c.getState())
        ).map(Relationship::getSource).findAny();
    }


    public ResourceCost getCost(){
        return new ResourceCost(cost, costTimeAmount, costTimeUnit);
    }

    public void updateCost(ResourceCost resourceCost){
        this.cost = resourceCost.cost();
        this.costTimeAmount = resourceCost.timeAmount();
        this.costTimeUnit = resourceCost.timeUnit();
    }

    public void synchronizeCost() {
        try {
            updateCost(getResourceHandler().getCurrentCost(this));
        }catch (Exception e){
            log.warn("Failed to synchronize cost for resource: {}.", name, e);
        }
    }

    public void transferTo(Long tenantId, Long ownerId, boolean enableCascadedTransfer){
        if(tenantId != null)
            transferToNewTenant(tenantId);
        if(ownerId != null)
            transferToNewOwner(ownerId);

        if(enableCascadedTransfer){
            for (Relationship capability : capabilities) {
                capability.getSource().transferTo(tenantId, ownerId, true);
            }
        }
    }

    public boolean exists() {
        return Utils.isNotBlank(externalId) &&
                ResourceState.getAliveStateSet().contains(state) &&
                syncState != ResourceSyncState.NOT_FOUND;
    }
}
