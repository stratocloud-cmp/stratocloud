package com.stratocloud.provider.tencent.common;

import com.stratocloud.provider.tencent.flavor.TencentFlavorId;
import com.tencentcloudapi.cvm.v20170312.models.Filter;

public class FilterFactory {
    public static Filter[] createFlavorFilter(TencentFlavorId flavorId) {
        Filter filter1 = createInstanceTypeFilter(flavorId.instanceType());
        Filter filter2 = createZoneFilter(flavorId.zone());
        return new Filter[]{filter1, filter2};
    }

    public static Filter createZoneFilter(String zone) {
        Filter filter = new Filter();
        filter.setName("zone");
        filter.setValues(new String[]{zone});
        return filter;
    }

    public static Filter createInstanceTypeFilter(String instanceType) {
        Filter filter = new Filter();
        filter.setName("instance-type");
        filter.setValues(new String[]{instanceType});
        return filter;
    }

    public static Filter createInstanceChargeTypeFilter(String instanceChargeType) {
        Filter filter = new Filter();
        filter.setName("instance-charge-type");
        filter.setValues(new String[]{instanceChargeType});
        return filter;
    }

}
