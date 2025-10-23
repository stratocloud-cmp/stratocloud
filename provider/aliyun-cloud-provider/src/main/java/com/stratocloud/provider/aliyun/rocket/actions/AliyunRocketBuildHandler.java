package com.stratocloud.provider.aliyun.rocket.actions;

import com.aliyun.rocketmq20220801.models.CreateInstanceRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.rocket.AliyunRocketHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class AliyunRocketBuildHandler implements BuildResourceActionHandler {

    private final AliyunRocketHandler rocketHandler;

    public AliyunRocketBuildHandler(AliyunRocketHandler rocketHandler) {
        this.rocketHandler = rocketHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return rocketHandler;
    }

    @Override
    public String getTaskName() {
        return "创建RocketMQ实例";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return AliyunRocketBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunCloudProvider provider = (AliyunCloudProvider) rocketHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunClient client = provider.buildClient(account);

        Resource vpcResource = resource.getEssentialTarget(ResourceCategories.VPC).orElseThrow(
                () -> new StratoException("VPC not provided")
        );

        List<String> vSwitchIds = resource.getRequirementTargets(ResourceCategories.SUBNET).stream().map(
                Resource::getExternalId
        ).toList();

        AliyunRocketBuildInput input = JSON.convert(parameters, AliyunRocketBuildInput.class);

        CreateInstanceRequest request = new CreateInstanceRequest();

        request.setInstanceName(resource.getName());
        request.setServiceCode("rmq");
        request.setSeriesCode(input.getSeriesCode());
        request.setSubSeriesCode(input.getSubSeriesCode());

        request.setPaymentType(input.getPaymentType());
        if(Objects.equals(input.getPaymentType(), "Subscription")){
            if(input.getPeriod()>=12){
                request.setPeriod(input.getPeriod().intValue()/12);
                request.setPeriodUnit("Year");
            }else {
                request.setPeriod(input.getPeriod().intValue());
                request.setPeriodUnit("Month");
            }

            request.setAutoRenew(input.isAutoRenew());
            if(input.isAutoRenew())
                request.setAutoRenewPeriod(input.getAutoRenewPeriod().intValue());
        }


        var networkInfo = getNetworkInfo(input, vpcResource, vSwitchIds);

        request.setNetworkInfo(networkInfo);

        var productInfo = getProductInfo(input);


        request.setProductInfo(productInfo);

        String instanceId = client.rocket().createInstance(request);
        resource.setExternalId(instanceId);
    }

    private static CreateInstanceRequest.CreateInstanceRequestNetworkInfo getNetworkInfo(AliyunRocketBuildInput input,
                                                                                         Resource vpcResource,
                                                                                         List<String> vSwitchIds) {
        var networkInfo = new CreateInstanceRequest.CreateInstanceRequestNetworkInfo();

        var internetInfo = new CreateInstanceRequest.CreateInstanceRequestNetworkInfoInternetInfo();
        internetInfo.setInternetSpec(input.getInternetSpec());
        if(Objects.equals(input.getInternetSpec(), "enable")){
            internetInfo.setFlowOutType(input.getFlowOutType());

            if(Objects.equals(input.getFlowOutType(), "payByBandwidth"))
                internetInfo.setFlowOutBandwidth(input.getFlowOutBandwidth());
        }else {
            internetInfo.setFlowOutType("uninvolved");
        }
        networkInfo.setInternetInfo(internetInfo);

        var vpcInfo = new CreateInstanceRequest.CreateInstanceRequestNetworkInfoVpcInfo();
        vpcInfo.setVpcId(vpcResource.getExternalId());
        vpcInfo.setVSwitches(vSwitchIds.stream().map(
                vSwitchId -> {
                    var vSwitch = new CreateInstanceRequest.CreateInstanceRequestNetworkInfoVpcInfoVSwitches();
                    vSwitch.setVSwitchId(vSwitchId);
                    return vSwitch;
                }
        ).toList());
        networkInfo.setVpcInfo(vpcInfo);
        return networkInfo;
    }

    private static CreateInstanceRequest.CreateInstanceRequestProductInfo getProductInfo(AliyunRocketBuildInput input) {
        var productInfo = new CreateInstanceRequest.CreateInstanceRequestProductInfo();
        productInfo.setMessageRetentionTime(input.getMessageRetentionTime());

        if(Objects.equals(input.getSeriesCode(), "professional")){
            productInfo.setAutoScaling(input.isAutoScaling());

            productInfo.setMsgProcessSpec(input.getProfessionalMsgProcessSpec());
            productInfo.setSendReceiveRatio(input.getSendReceiveRatio());

            productInfo.setStorageEncryption(false);
        } else if (Objects.equals(input.getSeriesCode(), "ultimate")) {
            productInfo.setAutoScaling(input.isAutoScaling());

            productInfo.setMsgProcessSpec(input.getUltimateMsgProcessSpec());
            productInfo.setSendReceiveRatio(input.getSendReceiveRatio());

            productInfo.setStorageEncryption(input.isStorageEncryption());

            if(input.isStorageEncryption())
                productInfo.setStorageSecretKey(input.getStorageSecretKey());
        }else {
            productInfo.setMsgProcessSpec(input.getStandardMsgProcessSpec());
            productInfo.setSendReceiveRatio(input.getSendReceiveRatio());

            productInfo.setStorageEncryption(false);
        }
        return productInfo;
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
