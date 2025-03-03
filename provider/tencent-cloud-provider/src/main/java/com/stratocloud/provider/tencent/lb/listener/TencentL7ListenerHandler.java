package com.stratocloud.provider.tencent.lb.listener;

import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.lb.common.TencentListenerHandler;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TencentL7ListenerHandler extends TencentListenerHandler {

    public TencentL7ListenerHandler(TencentCloudProvider provider) {
        super(provider);
    }

    @Override
    protected boolean listenerFilter(TencentListener listener) {
        return Set.of(
                "HTTP", "HTTPS"
        ).contains(listener.listener().getProtocol());
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_L7_LISTENER";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云七层监听器";
    }
}
