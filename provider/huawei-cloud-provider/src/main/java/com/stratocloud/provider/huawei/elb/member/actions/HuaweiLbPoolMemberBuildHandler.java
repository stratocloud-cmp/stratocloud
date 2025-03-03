package com.stratocloud.provider.huawei.elb.member.actions;

import com.huaweicloud.sdk.elb.v3.model.CreateMemberOption;
import com.huaweicloud.sdk.elb.v3.model.CreateMemberRequest;
import com.huaweicloud.sdk.elb.v3.model.CreateMemberRequestBody;
import com.huaweicloud.sdk.vpc.v2.model.Subnet;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.elb.member.HuaweiLbPoolMemberHandler;
import com.stratocloud.provider.huawei.elb.member.MemberId;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class HuaweiLbPoolMemberBuildHandler implements BuildResourceActionHandler {

    private final HuaweiLbPoolMemberHandler memberHandler;

    public HuaweiLbPoolMemberBuildHandler(HuaweiLbPoolMemberHandler memberHandler) {
        this.memberHandler = memberHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return memberHandler;
    }

    @Override
    public String getTaskName() {
        return "添加后端服务器组成员";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return HuaweiLbPoolMemberBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiLbPoolMemberBuildInput input = JSON.convert(parameters, HuaweiLbPoolMemberBuildInput.class);

        Resource subnetResource = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not provided when creating LB pool member.")
        );

        Resource poolResource = resource.getEssentialTarget(ResourceCategories.LOAD_BALANCER_BACKEND_GROUP).orElseThrow(
                () -> new StratoException("LB pool not provided when creating LB pool member.")
        );

        HuaweiCloudProvider provider = (HuaweiCloudProvider) memberHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudClient client = provider.buildClient(account);

        Subnet subnet = client.vpc().describeSubnet(subnetResource.getExternalId()).orElseThrow(
                () -> new StratoException("Subnet not found when creating LB pool member.")
        );

        CreateMemberOption memberOption = new CreateMemberOption()
                .withName(resource.getName())
                .withWeight(input.getWeight())
                .withProtocolPort(input.getBackendPort())
                .withAddress(input.getAddress())
                .withSubnetCidrId(subnet.getNeutronSubnetId());

        MemberId memberId = client.elb().createLbPoolMember(
                new CreateMemberRequest().withPoolId(poolResource.getExternalId()).withBody(
                        new CreateMemberRequestBody().withMember(memberOption)
                )
        );
        resource.setExternalId(memberId.toString());
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
