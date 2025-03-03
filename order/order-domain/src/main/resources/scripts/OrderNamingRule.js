function getNamePrefix(tenant, owner, jobDefinitions, jobParameters) {
    return tenant.name + "-";
}
getNamePrefix(args.tenant, args.owner, args.jobDefinitions, args.jobParameters);