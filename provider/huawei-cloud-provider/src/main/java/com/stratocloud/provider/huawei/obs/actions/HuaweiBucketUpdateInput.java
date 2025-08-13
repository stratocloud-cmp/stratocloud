package com.stratocloud.provider.huawei.obs.actions;

import com.stratocloud.provider.huawei.obs.HuaweiBucketSpec;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class HuaweiBucketUpdateInput extends HuaweiBucketSpec implements ResourceActionInput {
}
