package com.stratocloud.limit;

import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.limit.cmd.NestedUsageLimitItem;
import com.stratocloud.limit.query.NestedLimitResponse;
import com.stratocloud.resource.cmd.create.NestedResourceTag;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class ResourceUsageLimitAssembler {

    public static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100L);

    public NestedLimitResponse toNestedLimitResponse(ResourceUsageLimit resourceUsageLimit) {
        NestedLimitResponse nestedLimitResponse = new NestedLimitResponse();

        EntityUtil.copyBasicFields(resourceUsageLimit, nestedLimitResponse);

        nestedLimitResponse.setName(resourceUsageLimit.getName());
        nestedLimitResponse.setDescription(resourceUsageLimit.getDescription());
        nestedLimitResponse.setDisabled(resourceUsageLimit.getDisabled());
        nestedLimitResponse.setOwnerIds(resourceUsageLimit.getOwnerIds());
        nestedLimitResponse.setProviderIds(resourceUsageLimit.getProviderIds());
        nestedLimitResponse.setAccountIds(resourceUsageLimit.getAccountIds());
        nestedLimitResponse.setResourceCategories(resourceUsageLimit.getResourceCategories());
        nestedLimitResponse.setTags(convertTags(resourceUsageLimit.getTags()));
        nestedLimitResponse.setItems(convertItems(resourceUsageLimit.getItems()));
        return nestedLimitResponse;
    }

    private List<NestedUsageLimitItem> convertItems(List<ResourceUsageLimitItem> items) {
        if(items == null)
            return new ArrayList<>();

        return items.stream().map(this::convertItem).toList();
    }

    private NestedUsageLimitItem convertItem(ResourceUsageLimitItem resourceUsageLimitItem) {
        NestedUsageLimitItem nestedUsageLimitItem = new NestedUsageLimitItem();
        nestedUsageLimitItem.setUsageType(resourceUsageLimitItem.getUsageType());
        nestedUsageLimitItem.setUsageTypeName(resourceUsageLimitItem.getUsageTypeName());


        BigDecimal usageValue = resourceUsageLimitItem.getUsageValue();
        usageValue = usageValue != null ? usageValue : BigDecimal.ZERO;
        nestedUsageLimitItem.setUsageValue(usageValue.toPlainString());

        BigDecimal limitValue = resourceUsageLimitItem.getLimitValue();
        limitValue = limitValue != null ? limitValue : BigDecimal.ZERO;
        nestedUsageLimitItem.setLimitValue(limitValue.toPlainString());

        if(Objects.equals(limitValue, BigDecimal.ZERO)){
            nestedUsageLimitItem.setPercentage(100);
        } else {
            BigDecimal percentage = usageValue.multiply(ONE_HUNDRED).divide(limitValue, RoundingMode.UP);
            int integerPart = Double.valueOf(Math.ceil(percentage.doubleValue())).intValue();
            nestedUsageLimitItem.setPercentage(integerPart);
        }

        return nestedUsageLimitItem;
    }

    private List<NestedResourceTag> convertTags(List<ResourceUsageLimitTag> tags) {
        if(tags == null)
            return new ArrayList<>();
        return tags.stream().map(this::convertTag).toList();
    }

    private NestedResourceTag convertTag(ResourceUsageLimitTag resourceUsageLimitTag) {
        NestedResourceTag nestedResourceTag = new NestedResourceTag();
        nestedResourceTag.setTagKey(resourceUsageLimitTag.getTagKey());
        nestedResourceTag.setTagKeyName(resourceUsageLimitTag.getTagKeyName());
        nestedResourceTag.setTagValue(resourceUsageLimitTag.getTagValue());
        nestedResourceTag.setTagValueName(resourceUsageLimitTag.getTagValueName());
        return nestedResourceTag;
    }
}
