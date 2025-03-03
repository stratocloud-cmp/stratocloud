package com.stratocloud.provider.huawei.elb.member;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.elb.HuaweiLbStatusTreeHelper;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class HuaweiLbPoolMemberHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    public HuaweiLbPoolMemberHandler(HuaweiCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_LB_POOL_MEMBER";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云后端服务器";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.LOAD_BALANCER_BACKEND;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeMember(account, externalId).map(
                m -> toExternalResource(account, m)
        );
    }

    private ExternalResource toExternalResource(ExternalAccount account, HuaweiMember member) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                member.memberId().toString(),
                member.detail().getName(),
                HuaweiLbStatusTreeHelper.getMemberState(member.detail())
        );
    }

    public Optional<HuaweiMember> describeMember(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();
        return provider.buildClient(account).elb().describeMember(MemberId.fromString(externalId));
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).elb().describeMembers().stream().map(
                m -> toExternalResource(account, m)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        HuaweiMember member = describeMember(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("LB member not found.")
        );

        resource.updateByExternal(toExternalResource(account, member));

        RuntimeProperty ipProperty = RuntimeProperty.ofDisplayInList(
                "ip", "IP地址", member.detail().getAddress(), member.detail().getAddress()
        );
        resource.addOrUpdateRuntimeProperty(ipProperty);

        RuntimeProperty portProperty = RuntimeProperty.ofDisplayInList(
                "port",
                "后端端口",
                member.detail().getProtocolPort().toString(),
                member.detail().getProtocolPort().toString()
        );
        resource.addOrUpdateRuntimeProperty(portProperty);

        RuntimeProperty weightProperty = RuntimeProperty.ofDisplayInList(
                "weight",
                "转发权重",
                member.detail().getWeight().toString(),
                member.detail().getWeight().toString()
        );
        resource.addOrUpdateRuntimeProperty(weightProperty);
    }



    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
