package com.stratocloud.provider.tencent.lb.rule;

import com.tencentcloudapi.clb.v20180317.models.RuleOutput;

public record TencentL7Rule(TencentL7RuleId ruleId, RuleOutput rule) {
}
