package com.stratocloud.provider.huawei.dcs;

import com.huaweicloud.sdk.dcs.v2.model.AttrsObject;
import com.huaweicloud.sdk.dcs.v2.model.FlavorsItems;
import com.stratocloud.utils.Utils;

import java.util.Objects;

public class HuaweiDcsUtil {
    public static String getShardingNum(FlavorsItems f) {
        return f.getAttrs().stream().filter(
                attr -> Objects.equals(attr.getName(), "sharding_num")
        ).findAny().map(AttrsObject::getValue).orElse(null);
    }

    public static String getFlavorName(FlavorsItems f) {
        if (Utils.isEmpty(f.getCapacity()))
            return f.getSpecCode();
        return "%s (%s)".formatted(
                f.getSpecCode(),
                String.join(",", f.getCapacity().stream().map(c -> c + "GB").toList())
        );
    }
}
