package com.stratocloud.provider.huawei.elb.rule;

import com.huaweicloud.sdk.elb.v3.model.L7Rule;

public record HuaweiRule(HuaweiRuleId id, L7Rule detail) {
}
