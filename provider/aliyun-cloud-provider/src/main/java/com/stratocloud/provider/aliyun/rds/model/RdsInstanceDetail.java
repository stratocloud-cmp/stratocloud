package com.stratocloud.provider.aliyun.rds.model;

import com.aliyun.rds20140815.models.DescribeDBInstanceAttributeResponseBody;
import com.aliyun.rds20140815.models.DescribeDBInstanceNetInfoResponseBody;

import java.util.List;

public record RdsInstanceDetail(RdsInstance instance,
                                DescribeDBInstanceAttributeResponseBody.DescribeDBInstanceAttributeResponseBodyItemsDBInstanceAttribute attributes,
                                List<DescribeDBInstanceNetInfoResponseBody.DescribeDBInstanceNetInfoResponseBodyDBInstanceNetInfosDBInstanceNetInfo> netInfo) {
}
