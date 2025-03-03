package com.stratocloud.provider.aliyun.lb.classic.acl;

import com.aliyun.slb20140515.models.DescribeAccessControlListsRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunClbAclHandler extends AbstractResourceHandler {

    private final AliyunCloudProvider provider;

    public AliyunClbAclHandler(AliyunCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_CLB_ACL";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云ACL";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.LOAD_BALANCER_ACL;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        Optional<AliyunClbAcl> acl = describeAcl(account, externalId);
        return acl.map(value -> toExternalResource(account, value));
    }

    private ExternalResource toExternalResource(ExternalAccount account, AliyunClbAcl acl) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                acl.detail().getAclId(),
                acl.detail().getAclName(),
                ResourceState.AVAILABLE
        );
    }

    private Optional<AliyunClbAcl> describeAcl(ExternalAccount account, String aclId) {
        if(Utils.isBlank(aclId))
            return Optional.empty();

        return provider.buildClient(account).clb().describeAcl(aclId);
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        DescribeAccessControlListsRequest request = new DescribeAccessControlListsRequest();
        return provider.buildClient(account).clb().describeAclList(request).stream().map(
                acl -> toExternalResource(account, acl)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<ExternalResource> acl = describeExternalResource(account, resource.getExternalId());

        if(acl.isEmpty())
            return;

        resource.updateByExternal(acl.get());
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
