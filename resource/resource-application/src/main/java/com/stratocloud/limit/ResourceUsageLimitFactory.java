package com.stratocloud.limit;

import com.stratocloud.limit.cmd.CreateLimitCmd;
import com.stratocloud.limit.cmd.NestedUsageLimitItem;
import com.stratocloud.resource.cmd.create.NestedResourceTag;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class ResourceUsageLimitFactory {
    public ResourceUsageLimit create(CreateLimitCmd cmd) {
        ResourceUsageLimit resourceUsageLimit = new ResourceUsageLimit();

        resourceUsageLimit.setTenantId(cmd.getTenantId());

        resourceUsageLimit.setName(cmd.getName());
        resourceUsageLimit.setDescription(cmd.getDescription());

        resourceUsageLimit.setOwnerIds(cmd.getOwnerIds());
        resourceUsageLimit.setProviderIds(cmd.getProviderIds());
        resourceUsageLimit.setAccountIds(cmd.getAccountIds());
        resourceUsageLimit.setResourceCategories(cmd.getResourceCategories());

        resourceUsageLimit.addTags(createLimitTags(cmd.getTags()));
        resourceUsageLimit.addItems(createLimitItems(cmd.getItems()));


        return resourceUsageLimit;
    }

    public List<ResourceUsageLimitItem> createLimitItems(List<NestedUsageLimitItem> items) {
        if(items == null)
            return new ArrayList<>();

        return items.stream().map(this::createLimitItem).toList();
    }

    private ResourceUsageLimitItem createLimitItem(NestedUsageLimitItem nestedUsageLimitItem) {
        ResourceUsageLimitItem resourceUsageLimitItem = new ResourceUsageLimitItem();
        resourceUsageLimitItem.setUsageType(nestedUsageLimitItem.getUsageType());
        resourceUsageLimitItem.setUsageTypeName(nestedUsageLimitItem.getUsageTypeName());
        resourceUsageLimitItem.setLimitValue(
                new BigDecimal(nestedUsageLimitItem.getLimitValue())
        );
        return resourceUsageLimitItem;
    }

    public List<ResourceUsageLimitTag> createLimitTags(List<NestedResourceTag> tags) {
        if(tags == null)
            return new ArrayList<>();
        return tags.stream().map(this::createLimitTag).toList();
    }

    private ResourceUsageLimitTag createLimitTag(NestedResourceTag nestedResourceTag) {
        ResourceUsageLimitTag resourceUsageLimitTag = new ResourceUsageLimitTag();
        resourceUsageLimitTag.setTagKey(nestedResourceTag.getTagKey());
        resourceUsageLimitTag.setTagKeyName(nestedResourceTag.getTagKeyName());
        resourceUsageLimitTag.setTagValue(nestedResourceTag.getTagValue());
        resourceUsageLimitTag.setTagValueName(nestedResourceTag.getTagValueName());
        return resourceUsageLimitTag;
    }
}
