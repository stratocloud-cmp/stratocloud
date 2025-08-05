package com.stratocloud.provider.aliyun.oss.actions;

import com.stratocloud.provider.aliyun.oss.AliyunBucketSpec;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AliyunBucketBuildInput extends AliyunBucketSpec implements ResourceActionInput {
}
