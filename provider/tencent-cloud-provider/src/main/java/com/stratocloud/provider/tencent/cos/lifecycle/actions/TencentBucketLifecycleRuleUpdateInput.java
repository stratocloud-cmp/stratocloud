package com.stratocloud.provider.tencent.cos.lifecycle.actions;

import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TencentBucketLifecycleRuleUpdateInput
        extends TencentBucketLifecycleRuleSpec implements ResourceActionInput {

}
