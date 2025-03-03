package com.stratocloud.provider.aliyun.securitygroup.policy;

import com.aliyun.ecs20140526.models.DescribeSecurityGroupAttributeResponseBody;

public record AliyunSecurityGroupPolicy(
        AliyunSecurityGroupPolicyId policyId,
        DescribeSecurityGroupAttributeResponseBody.DescribeSecurityGroupAttributeResponseBodyPermissionsPermission detail
) {
    public boolean isEgress() {
        return "egress".equals(detail.getDirection());
    }

    public boolean isIngress() {
        return "ingress".equals(detail.getDirection());
    }
}
