package com.stratocloud.provider.tencent.lb.listener;

import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.lb.common.TencentListenerHandler;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TencentL4ListenerHandler extends TencentListenerHandler {

    public TencentL4ListenerHandler(TencentCloudProvider provider) {
        super(provider);
    }

    @Override
    protected boolean listenerFilter(TencentListener listener) {
        return Set.of(
                "TCP", "UDP", "TCP_SSL", "QUIC"
        ).contains(listener.listener().getProtocol());
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_L4_LISTENER";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云四层监听器";
    }
}
