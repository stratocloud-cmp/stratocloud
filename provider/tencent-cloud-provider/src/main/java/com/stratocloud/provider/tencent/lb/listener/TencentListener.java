package com.stratocloud.provider.tencent.lb.listener;

import com.tencentcloudapi.clb.v20180317.models.Listener;

public record TencentListener(String loadBalancerId, Listener listener) {
}
