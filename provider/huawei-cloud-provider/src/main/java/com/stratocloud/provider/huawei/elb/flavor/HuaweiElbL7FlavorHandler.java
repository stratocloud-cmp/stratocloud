package com.stratocloud.provider.huawei.elb.flavor;

import com.huaweicloud.sdk.elb.v3.model.Flavor;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class HuaweiElbL7FlavorHandler extends HuaweiElbFlavorHandler {

    public HuaweiElbL7FlavorHandler(HuaweiCloudProvider provider) {
        super(provider);
    }

    @Override
    protected boolean filterFlavor(Flavor flavor) {
        return Set.of(
                "L7","L7_elastic","L7_elastic_max"
        ).contains(flavor.getType());
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_ELB_L7_FLAVOR";
    }

    @Override
    public String getResourceTypeName() {
        return "华为ELB应用型规格";
    }
}
