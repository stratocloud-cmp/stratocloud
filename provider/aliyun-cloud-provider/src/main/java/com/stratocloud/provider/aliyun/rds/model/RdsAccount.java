package com.stratocloud.provider.aliyun.rds.model;

import com.aliyun.rds20140815.models.DescribeAccountsResponseBody;

public record RdsAccount(DescribeAccountsResponseBody.DescribeAccountsResponseBodyAccountsDBInstanceAccount detail) {
}
