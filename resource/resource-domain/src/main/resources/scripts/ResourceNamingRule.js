function getNamePrefix(tenant, owner, resource, resourceCategoryAbbr) {
    return resourceCategoryAbbr + "-";
}

getNamePrefix(args.tenant, args.owner, args.resource, args.resourceCategoryAbbr);