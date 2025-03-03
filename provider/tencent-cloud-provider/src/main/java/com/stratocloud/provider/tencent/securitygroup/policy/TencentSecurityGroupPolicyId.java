package com.stratocloud.provider.tencent.securitygroup.policy;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.provider.constants.SecurityGroupPolicyDirection;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.vpc.v20170312.models.SecurityGroupPolicy;

public record TencentSecurityGroupPolicyId(String securityGroupId,
                                           SecurityGroupPolicyDirection policyDirection,
                                           InternetProtocol internetProtocol,
                                           String cidrBlock,
                                           String action,
                                           String protocol,
                                           String port) {

    private static final String POLICY_ID_TEMPLATE = "%s-%s-%s-%s-%s-%s@%s";


    @Override
    public String toString() {
        return POLICY_ID_TEMPLATE.formatted(
                policyDirection, internetProtocol, cidrBlock, action, protocol, port, securityGroupId
        );
    }

    public static TencentSecurityGroupPolicyId fromString(String s){
        try {
            String[] arr = s.split("@");
            String prefix = arr[0];
            String securityGroupId = arr[1];

            String[] arr2 = prefix.split("-");

            SecurityGroupPolicyDirection policyDirection = SecurityGroupPolicyDirection.valueOf(arr2[0]);
            InternetProtocol ipVersion = InternetProtocol.valueOf(arr2[1]);
            String cidrBlock = arr2[2];
            String action = arr2[3];
            String protocol = arr2[4];
            String port = arr2[5];

            return new TencentSecurityGroupPolicyId(
                    securityGroupId,
                    policyDirection,
                    ipVersion,
                    cidrBlock,
                    action,
                    protocol,
                    port
            );
        }catch (Exception e){
            throw new StratoException("Failed to parse TencentSecurityGroupPolicyId: "+s);
        }
    }

    public static TencentSecurityGroupPolicyId fromPolicy(String securityGroupId,
                                                          SecurityGroupPolicyDirection policyDirection,
                                                          SecurityGroupPolicy policy){
        InternetProtocol ipVersion = getIpVersion(policy);
        String cidr = getCidr(policy);

        if(ipVersion == null)
            throw new StratoException("Cannot resolve internet protocol from policy.");

        if(cidr == null)
            throw new StratoException("Cannot resolve cidr from policy.");

        return new TencentSecurityGroupPolicyId(
                securityGroupId,
                policyDirection,
                ipVersion,
                cidr.toLowerCase(),
                policy.getAction().toLowerCase(),
                policy.getProtocol().toLowerCase(),
                policy.getPort().toLowerCase()
        );
    }

    private static InternetProtocol getIpVersion(SecurityGroupPolicy policy){
        if(Utils.isNotBlank(policy.getIpv6CidrBlock()))
            return InternetProtocol.IPv6;
        else if(Utils.isNotBlank(policy.getCidrBlock()))
            return InternetProtocol.IPv4;

        return null;
    }

    private static String getCidr(SecurityGroupPolicy policy){
        if(Utils.isNotBlank(policy.getIpv6CidrBlock()))
            return policy.getIpv6CidrBlock();
        else if(Utils.isNotBlank(policy.getCidrBlock()))
            return policy.getCidrBlock();

        return null;
    }

    public boolean isSamePolicy(SecurityGroupPolicy policy){
        InternetProtocol ipVersion = getIpVersion(policy);
        String cidr = getCidr(policy);

        return internetProtocol == ipVersion
                && cidrBlock.equalsIgnoreCase(cidr)
                && action.equalsIgnoreCase(policy.getAction())
                && protocol.equalsIgnoreCase(policy.getProtocol())
                && port.equalsIgnoreCase(policy.getPort());
    }
}
