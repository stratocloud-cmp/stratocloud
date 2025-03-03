package com.stratocloud.resource;

public record ResourceCategory(ResourceCategoryGroup group,
                               String id,
                               String name,
                               String abbr,
                               int index) {

}
