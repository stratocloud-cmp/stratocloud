package com.stratocloud.provider.aliyun.rds.model;

import com.aliyun.rds20140815.models.DescribeDBInstancesResponseBody;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stratocloud.provider.aliyun.common.AliyunTimeUtil;
import com.stratocloud.utils.Utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record RdsInstance(DescribeDBInstancesResponseBody.DescribeDBInstancesResponseBodyItemsDBInstance detail) {


    @JsonIgnore
    public int getMonthPeriod() {
        if(!Objects.equals(detail.getPayType(), "Prepaid"))
            return 0;

        if(Utils.isBlank(detail.getCreateTime()) || Utils.isBlank(detail.getExpireTime()))
            return 0;

        LocalDateTime createTime = AliyunTimeUtil.toLocalDateSecondsTime(detail.getCreateTime());
        LocalDateTime expireTime = AliyunTimeUtil.toLocalDateSecondsTime(detail.getExpireTime());

        return (int) createTime.until(expireTime, ChronoUnit.MONTHS);
    }
}
