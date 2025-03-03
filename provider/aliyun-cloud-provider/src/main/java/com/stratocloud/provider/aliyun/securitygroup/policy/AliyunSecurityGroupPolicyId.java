package com.stratocloud.provider.aliyun.securitygroup.policy;

import com.stratocloud.exceptions.StratoException;

public record AliyunSecurityGroupPolicyId(String securityGroupId, String ruleId) {

    @Override
    public String toString() {
        return "%s@%s".formatted(ruleId, securityGroupId);
    }

    public static AliyunSecurityGroupPolicyId fromString(String s){
        try {
            String[] arr = s.split("@");
            return new AliyunSecurityGroupPolicyId(arr[1], arr[0]);
        }catch (Exception e){
            throw new StratoException("Failed to parse AliyunSecurityGroupPolicyId: "+s);
        }
    }
}
