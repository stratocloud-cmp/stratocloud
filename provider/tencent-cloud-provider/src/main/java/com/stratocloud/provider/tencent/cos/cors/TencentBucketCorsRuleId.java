package com.stratocloud.provider.tencent.cos.cors;

public record TencentBucketCorsRuleId(String bucketName, String ruleId) {
    @Override
    public String toString() {
        return ruleId+"@"+bucketName;
    }

    public static TencentBucketCorsRuleId fromString(String externalId){
        String[] split = externalId.split("@");
        return new TencentBucketCorsRuleId(split[1], split[0]);
    }
}
