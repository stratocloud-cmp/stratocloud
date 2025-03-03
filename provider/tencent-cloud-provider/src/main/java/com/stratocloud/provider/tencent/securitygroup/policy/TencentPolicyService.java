package com.stratocloud.provider.tencent.securitygroup.policy;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.SecurityGroupPolicyDirection;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.securitygroup.policy.actions.TencentEgressPolicyBuildInput;
import com.stratocloud.provider.tencent.securitygroup.policy.actions.TencentIngressPolicyBuildInput;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.vpc.v20170312.models.CreateSecurityGroupPoliciesRequest;
import com.tencentcloudapi.vpc.v20170312.models.SecurityGroupPolicy;
import com.tencentcloudapi.vpc.v20170312.models.SecurityGroupPolicySet;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TencentPolicyService {

    private final TencentCloudProvider provider;

    public TencentPolicyService(TencentCloudProvider provider) {
        this.provider = provider;
    }


    public void createPolicy(Resource policyResource, SecurityGroupPolicyDirection direction){
        Resource securityGroup = policyResource.getRequirementTargets(
                ResourceCategories.SECURITY_GROUP
        ).stream().findAny().orElseThrow(
                () -> new StratoException("Policy requires security group.")
        );

        Map<String, Object> properties = policyResource.getProperties();

        ExternalAccount account = provider.getAccountRepository().findExternalAccount(policyResource.getAccountId());


        TencentCloudClient client = provider.buildClient(account);


        SecurityGroupPolicySet policySet = new SecurityGroupPolicySet();

        SecurityGroupPolicy policy;
        switch (direction){
            case ingress -> {
                var input = JSON.convert(properties, TencentIngressPolicyBuildInput.class);
                policy = input.createPolicy();
                policySet.setIngress(new SecurityGroupPolicy[]{policy});
            }
            case egress -> {
                var input = JSON.convert(properties, TencentEgressPolicyBuildInput.class);
                policy = input.createPolicy();
                policySet.setEgress(new SecurityGroupPolicy[]{policy});
            }
            default -> throw new StratoException("Unknown policy direction: "+direction);
        }

        CreateSecurityGroupPoliciesRequest request = new CreateSecurityGroupPoliciesRequest();
        request.setSecurityGroupId(securityGroup.getExternalId());
        request.setSecurityGroupPolicySet(policySet);

        client.createSecurityGroupPolicies(request);

        TencentSecurityGroupPolicyId policyId = TencentSecurityGroupPolicyId.fromPolicy(
                securityGroup.getExternalId(),
                direction,
                policy
        );

        policyResource.setExternalId(policyId.toString());
    }

    public void removePolicy(Resource policyResource){
        if(Utils.isBlank(policyResource.getExternalId()))
            return;

        var policyId = TencentSecurityGroupPolicyId.fromString(policyResource.getExternalId());

        ExternalAccount account = provider.getAccountRepository().findExternalAccount(policyResource.getAccountId());

        TencentCloudClient client = provider.buildClient(account);

        client.removeSecurityGroupPolicy(policyId);
    }


}
