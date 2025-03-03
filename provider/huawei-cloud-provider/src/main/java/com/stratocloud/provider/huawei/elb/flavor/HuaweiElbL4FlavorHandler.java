package com.stratocloud.provider.huawei.elb.flavor;

import com.huaweicloud.sdk.elb.v3.model.Flavor;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class HuaweiElbL4FlavorHandler extends HuaweiElbFlavorHandler {

    public HuaweiElbL4FlavorHandler(HuaweiCloudProvider provider) {
        super(provider);
    }

    @Override
    protected boolean filterFlavor(Flavor flavor) {
        return Set.of(
                "L4","L4_elastic","L4_elastic_max"
        ).contains(flavor.getType());
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_ELB_L4_FLAVOR";
    }

    @Override
    public String getResourceTypeName() {
        return "华为ELB网络型规格";
    }
}
