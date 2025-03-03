package com.stratocloud.limit;

import com.stratocloud.exceptions.ResourceUsageReachLimitException;
import com.stratocloud.job.Task;
import com.stratocloud.job.TaskTargetEntity;
import com.stratocloud.jpa.entities.Tenanted;
import com.stratocloud.limit.task.SynchronizeLimitTaskHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.Utils;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
public class ResourceUsageLimit extends Tenanted implements TaskTargetEntity {
    @Column
    private String name;
    @Column
    private String description;
    @Column
    private Boolean disabled = false;
    @Column
    private List<Long> ownerIds = new ArrayList<>();
    @Column
    private List<String> providerIds = new ArrayList<>();
    @Column
    private List<Long> accountIds = new ArrayList<>();
    @Column
    private List<String> resourceCategories = new ArrayList<>();
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "limit", orphanRemoval = true)
    private List<ResourceUsageLimitTag> tags = new ArrayList<>();
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "limit", orphanRemoval = true)
    private List<ResourceUsageLimitItem> items = new ArrayList<>();

    private boolean canMatch(Resource resource){
        if(disabled)
            return false;

        if(Utils.isNotEmpty(ownerIds) && !ownerIds.contains(resource.getOwnerId()))
            return false;

        if(Utils.isNotEmpty(providerIds) && !providerIds.contains(resource.getProviderId()))
            return false;

        if(Utils.isNotEmpty(accountIds) && !accountIds.contains(resource.getAccountId()))
            return false;

        if(Utils.isNotEmpty(resourceCategories) && !resourceCategories.contains(resource.getCategory()))
            return false;

        if(Utils.isNotEmpty(tags)){
            return tags.stream().allMatch(t -> resource.hasTag(t.getTagKey(), t.getTagValue()));
        }

        return true;
    }


    public void check(List<Resource> resourcesToAllocate){
        if(Utils.isEmpty(resourcesToAllocate))
            return;

        Set<Resource> matchedResources = resourcesToAllocate.stream().filter(
                this::canMatch
        ).collect(Collectors.toSet());

        if(Utils.isEmpty(matchedResources))
            return;

        for (ResourceUsageLimitItem item : items) {
            if(item.getLimitValue() == null)
                continue;
            BigDecimal total = BigDecimal.ZERO;
            if(item.getUsageValue() != null)
                total = total.add(item.getUsageValue());
            for (Resource matchedResource : matchedResources) {
                total = total.add(matchedResource.getPreAllocatedUsageByType(item.getUsageType()));
            }
            if(total.compareTo(item.getLimitValue()) > 0)
                throw new ResourceUsageReachLimitException("已到达配额上限: %s".formatted(name));
        }
    }

    public Map<String, List<String>> getTagsMap() {
        Map<String, List<String>> map = new HashMap<>();

        for (ResourceUsageLimitTag tag : tags) {
            map.computeIfAbsent(tag.getTagKey(), k->new ArrayList<>());
            map.get(tag.getTagKey()).add(tag.getTagValue());
        }

        return map;
    }

    public void addTags(List<ResourceUsageLimitTag> limitTags) {
        limitTags.forEach(t -> t.setLimit(this));
        tags.addAll(limitTags);
    }

    public void addItems(List<ResourceUsageLimitItem> limitItems) {
        limitItems.forEach(it -> it.setLimit(this));
        items.addAll(limitItems);
    }

    public void update(String name, String description, List<Long> ownerIds, List<String> providerIds,
                       List<Long> accountIds, List<String> resourceCategories) {
        this.name = name;
        this.description = description;
        this.ownerIds = ownerIds;
        this.providerIds = providerIds;
        this.accountIds = accountIds;
        this.resourceCategories = resourceCategories;
    }

    public void clearTags() {
        tags.clear();
    }

    public void clearItems() {
        items.clear();
    }

    public void enable() {
        this.disabled = false;
    }

    public void disable() {
        this.disabled = true;
    }

    public void synchronize(List<Resource> resources) {
        if(Utils.isEmpty(items))
            return;

        for (ResourceUsageLimitItem item : items) {
            BigDecimal total = BigDecimal.ZERO;

            if(Utils.isNotEmpty(resources)){
                for (Resource resource : resources) {
                    if(canMatch(resource))
                        total = total.add(resource.getTotalUsageByType(item.getUsageType()));
                }
            }

            item.setUsageValue(total);
        }
    }

    public Task createSynchronizeTask() {
        return new Task(
                this,
                SynchronizeLimitTaskHandler.TASK_TYPE,
                new SynchronizeResourceUsageLimitInputs(getId())
        );
    }

    @Override
    public String getEntityDescription() {
        String result = name;
        if(Utils.isNotEmpty(items)){
            var usageTypeNames = items.stream().map(ResourceUsageLimitItem::getUsageTypeName).toList();
            result = "%s[%s]".formatted(result, String.join(",", usageTypeNames));
        }
        return result;
    }
}
