package com.stratocloud.provider.aliyun.redis.model;

import com.aliyun.r_kvstore20150101.models.DescribeInstanceAttributeResponseBody;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stratocloud.provider.aliyun.common.AliyunTimeUtil;
import com.stratocloud.utils.Utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record RedisInstanceAttribute(DescribeInstanceAttributeResponseBody.DescribeInstanceAttributeResponseBodyInstancesDBInstanceAttribute detail) {

    @JsonIgnore
    public int getMonthPeriod() {
        if(!Objects.equals(detail.getChargeType(), "PrePaid"))
            return 0;

        if(Utils.isBlank(detail.getCreateTime()) || Utils.isBlank(detail.getEndTime()))
            return 0;

        LocalDateTime createTime = AliyunTimeUtil.toLocalDateSecondsTime(detail.getCreateTime());
        LocalDateTime expireTime = AliyunTimeUtil.toLocalDateSecondsTime(detail.getEndTime());

        return (int) createTime.until(expireTime, ChronoUnit.MONTHS);
    }
}
