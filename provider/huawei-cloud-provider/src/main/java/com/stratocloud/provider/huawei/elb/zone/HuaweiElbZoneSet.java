package com.stratocloud.provider.huawei.elb.zone;

import com.huaweicloud.sdk.elb.v3.model.AvailabilityZone;

import java.util.List;
import java.util.Objects;

public record HuaweiElbZoneSet(List<AvailabilityZone> zoneSet) {

    public String getZoneSetId(){
        List<String> zoneCodes = zoneSet.stream().map(AvailabilityZone::getCode).distinct().sorted(String::compareTo).toList();
        return String.join(",", zoneCodes);
    }

    public String getZoneSetName(){
        return getZoneSetId();
    }

    public boolean containsZone(String zoneCode) {
        return zoneSet.stream().anyMatch(
                z -> Objects.equals(zoneCode, z.getCode())
        );
    }

    public boolean containsAll(List<String> zoneCodes) {
        return zoneCodes.stream().allMatch(this::containsZone);
    }
}
