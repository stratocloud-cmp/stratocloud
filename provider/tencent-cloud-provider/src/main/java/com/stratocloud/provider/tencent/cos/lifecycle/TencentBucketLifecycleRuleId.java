package com.stratocloud.provider.tencent.cos.lifecycle;

public record TencentBucketLifecycleRuleId(String bucketName, String ruleId) {
    @Override
    public String toString() {
        return ruleId+"@"+bucketName;
    }

    public static TencentBucketLifecycleRuleId fromString(String externalId){
        String[] split = externalId.split("@");
        return new TencentBucketLifecycleRuleId(split[1], split[0]);
    }
}
