package com.stratocloud.provider.aliyun.lb.classic.backend.msgroup;

import com.aliyun.slb20140515.models.DescribeMasterSlaveServerGroupAttributeResponseBody;
import com.aliyun.slb20140515.models.DescribeMasterSlaveServerGroupsResponseBody;

public record AliyunClbMasterSlaveGroup(
        AliyunClbMasterSlaveGroupId id,
        DescribeMasterSlaveServerGroupsResponseBody.DescribeMasterSlaveServerGroupsResponseBodyMasterSlaveServerGroupsMasterSlaveServerGroup detail,
        DescribeMasterSlaveServerGroupAttributeResponseBody attributes
) {
}
