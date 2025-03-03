package com.stratocloud.provider.huawei.subnet;

import com.huaweicloud.sdk.vpc.v2.model.ListSubnetsRequest;
import com.huaweicloud.sdk.vpc.v2.model.Subnet;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class HuaweiSubnetHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    public HuaweiSubnetHandler(HuaweiCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_SUBNET";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云子网";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.SUBNET;
    }

    @Override
    public boolean isInfrastructure() {
        return true;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        Optional<Subnet> subnet = describeSubnet(account, externalId);
        return subnet.map(
                s->toExternalResource(account, s)
        );
    }

    public ExternalResource toExternalResource(ExternalAccount account, Subnet subnet) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                subnet.getId(),
                subnet.getName(),
                convertStatus(subnet.getStatus())
        );
    }

    private ResourceState convertStatus(Subnet.StatusEnum status) {
        if(status.equals(Subnet.StatusEnum.ACTIVE))
            return ResourceState.AVAILABLE;
        else if(status.equals(Subnet.StatusEnum.UNKNOWN))
            return ResourceState.UNKNOWN;
        else if(status.equals(Subnet.StatusEnum.ERROR))
            return ResourceState.ERROR;
        else
            return ResourceState.UNKNOWN;
    }

    public Optional<Subnet> describeSubnet(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).vpc().describeSubnet(externalId);
    }

    public Optional<Subnet> describeSubnetByNeutronSubnetId(ExternalAccount account,
                                                            String neutronSubnetId){
        ListSubnetsRequest request = new ListSubnetsRequest();
        return provider.buildClient(account).vpc().describeSubnets(request).stream().filter(
                s -> Objects.equals(neutronSubnetId, s.getNeutronSubnetId())
        ).findAny();
    }

    public Optional<Subnet> describeSubnetByNeutronNetworkId(ExternalAccount account,
                                                             String neutronNetworkId){
        ListSubnetsRequest request = new ListSubnetsRequest();
        return provider.buildClient(account).vpc().describeSubnets(request).stream().filter(
                s -> Objects.equals(neutronNetworkId, s.getNeutronNetworkId())
        ).findAny();
    }

    public List<Subnet> describeSubnetsByNeutronNetworkIds(ExternalAccount account,
                                                           List<String> neutronNetworkIds){
        ListSubnetsRequest request = new ListSubnetsRequest();
        return provider.buildClient(account).vpc().describeSubnets(request).stream().filter(
                s -> neutronNetworkIds.contains(s.getNeutronNetworkId())
        ).toList();
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        ListSubnetsRequest request = new ListSubnetsRequest();
        return provider.buildClient(account).vpc().describeSubnets(request).stream().map(
                s -> toExternalResource(account,s)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Subnet subnet = describeSubnet(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Subnet not found")
        );

        resource.updateByExternal(toExternalResource(account, subnet));


        String cidr = subnet.getCidr();

        RuntimeProperty cidrProp = RuntimeProperty.ofDisplayInList(
                "cidr", "CIDR", cidr, cidr
        );

        resource.addOrUpdateRuntimeProperty(cidrProp);


        String cidrV6 = subnet.getCidrV6();

        if(Utils.isNotBlank(cidrV6)){
            RuntimeProperty cidrV6Prop = RuntimeProperty.ofDisplayInList(
                    "cidrV6", "CIDR(IPv6)", cidrV6, cidrV6
            );
            resource.addOrUpdateRuntimeProperty(cidrV6Prop);
        }

        String gatewayIp = subnet.getGatewayIp();

        if(Utils.isNotBlank(gatewayIp)){
            RuntimeProperty gatewayIpProp = RuntimeProperty.ofDisplayable(
                    "gatewayIp", "网关", gatewayIp, gatewayIp
            );
            resource.addOrUpdateRuntimeProperty(gatewayIpProp);
        }

        String gatewayIpV6 = subnet.getGatewayIpV6();

        if(Utils.isNotBlank(gatewayIpV6)){
            RuntimeProperty gatewayIpV6Prop = RuntimeProperty.ofDisplayable(
                    "gatewayIpV6", "网关(IPv6)", gatewayIp, gatewayIp
            );
            resource.addOrUpdateRuntimeProperty(gatewayIpV6Prop);
        }

    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }

    @Override
    public boolean canAttachIpPool() {
        return true;
    }
}
