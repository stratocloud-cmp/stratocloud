package com.stratocloud.provider.huawei.elb.rule;

import com.stratocloud.exceptions.StratoException;

public record HuaweiRuleId(String policyId, String ruleId) {

    @Override
    public String toString() {
        return "%s@%s".formatted(ruleId, policyId);
    }

    public static HuaweiRuleId fromString(String s){
        try {
            String[] arr = s.split("@");
            return new HuaweiRuleId(arr[1], arr[0]);
        }catch (Exception e){
            throw new StratoException("Failed to parse HuaweiRuleId: "+s);
        }
    }

}
