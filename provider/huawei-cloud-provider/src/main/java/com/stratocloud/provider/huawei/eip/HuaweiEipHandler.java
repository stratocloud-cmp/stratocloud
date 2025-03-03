package com.stratocloud.provider.huawei.eip;

import com.huaweicloud.sdk.eip.v2.model.ListPublicipsRequest;
import com.huaweicloud.sdk.eip.v2.model.PublicipShowResp;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class HuaweiEipHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;


    public HuaweiEipHandler(HuaweiCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_EIP";
    }

    @Override
    public String getResourceTypeName() {
        return "华为弹性IP";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.ELASTIC_IP;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }


    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeEip(account, externalId).map(
                eip -> toExternalResource(account, eip)
        );
    }

    public Optional<PublicipShowResp> describeEip(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).eip().describeEip(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, PublicipShowResp eip) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                eip.getId(),
                eip.getPublicIpAddress(),
                convertState(eip)
        );
    }

    private ResourceState convertState(PublicipShowResp eip) {
        PublicipShowResp.StatusEnum status = eip.getStatus();
        if(PublicipShowResp.StatusEnum.ACTIVE.equals(status))
            return ResourceState.IN_USE;
        else if(PublicipShowResp.StatusEnum.DOWN.equals(status))
            return ResourceState.IDLE;
        else if(PublicipShowResp.StatusEnum.FREEZED.equals(status))
            return ResourceState.UNAVAILABLE;
        else if(PublicipShowResp.StatusEnum.BIND_ERROR.equals(status))
            return ResourceState.ERROR;
        else if(PublicipShowResp.StatusEnum.BINDING.equals(status))
            return ResourceState.ATTACHING;
        else if(PublicipShowResp.StatusEnum.PENDING_DELETE.equals(status))
            return ResourceState.DESTROYING;
        else if(PublicipShowResp.StatusEnum.PENDING_CREATE.equals(status))
            return ResourceState.BUILDING;
        else if(PublicipShowResp.StatusEnum.PENDING_UPDATE.equals(status))
            return ResourceState.CONFIGURING;
        else if(PublicipShowResp.StatusEnum.ELB.equals(status))
            return ResourceState.IN_USE;
        else if(PublicipShowResp.StatusEnum.ERROR.equals(status))
            return ResourceState.ERROR;
        else if(PublicipShowResp.StatusEnum.VPN.equals(status))
            return ResourceState.IN_USE;
        else
            return ResourceState.UNKNOWN;
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        var client = provider.buildClient(account);
        return client.eip().describeEips(new ListPublicipsRequest()).stream().map(
                eip -> toExternalResource(account, eip)
        ).toList();
    }


    @Override
    public void synchronize(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return;

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        PublicipShowResp eip = describeEip(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("EIP not found: " + resource.getName())
        );

        resource.updateByExternal(toExternalResource(account, eip));

        RuntimeProperty publicIpProp = RuntimeProperty.ofDisplayInList(
                "publicIp",
                "公网IP",
                eip.getPublicIpAddress(),
                eip.getPublicIpAddress()
        );
        resource.addOrUpdateRuntimeProperty(publicIpProp);


        if(Utils.isNotBlank(eip.getPrivateIpAddress())){
            RuntimeProperty privateIpProperty = RuntimeProperty.ofDisplayInList(
                    "privateIp",
                    "内网IP",
                    eip.getPrivateIpAddress(),
                    eip.getPrivateIpAddress()
            );
            resource.addOrUpdateRuntimeProperty(privateIpProperty);
        }

        resource.updateUsageByType(UsageTypes.ELASTIC_IP, BigDecimal.ONE);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of(
                UsageTypes.ELASTIC_IP
        );
    }
}
