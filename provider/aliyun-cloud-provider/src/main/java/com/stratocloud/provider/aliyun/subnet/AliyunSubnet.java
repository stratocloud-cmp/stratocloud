package com.stratocloud.provider.aliyun.subnet;

import com.aliyun.vpc20160428.models.DescribeVSwitchesResponseBody;

public record AliyunSubnet(DescribeVSwitchesResponseBody.DescribeVSwitchesResponseBodyVSwitchesVSwitch detail) {
}
