package com.stratocloud.provider.tencent.lb.backend;

import com.tencentcloudapi.clb.v20180317.models.Backend;

public record TencentBackend(String lbId, String listenerId, Backend backend) {
}
