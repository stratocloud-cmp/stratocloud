package com.stratocloud.provider.tencent.cos.cors;

import com.qcloud.cos.model.CORSRule;

public record TencentBucketCorsRule(TencentBucketCorsRuleId id, CORSRule detail) {
}
