package com.stratocloud.provider.aliyun.eip;

import com.aliyun.vpc20160428.models.DescribeEipAddressesResponseBody;

public record AliyunEip(
        DescribeEipAddressesResponseBody.DescribeEipAddressesResponseBodyEipAddressesEipAddress detail
) {
}
