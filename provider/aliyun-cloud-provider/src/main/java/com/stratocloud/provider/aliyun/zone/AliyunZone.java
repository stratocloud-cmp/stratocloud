package com.stratocloud.provider.aliyun.zone;

import com.aliyun.ecs20140526.models.DescribeAvailableResourceResponseBody;
import com.aliyun.ecs20140526.models.DescribeZonesResponseBody;

public record AliyunZone(DescribeZonesResponseBody.DescribeZonesResponseBodyZonesZone zone,
                         DescribeAvailableResourceResponseBody.DescribeAvailableResourceResponseBodyAvailableZonesAvailableZone availability) {
    public String getZoneId(){
        return zone.getZoneId();
    }
}
