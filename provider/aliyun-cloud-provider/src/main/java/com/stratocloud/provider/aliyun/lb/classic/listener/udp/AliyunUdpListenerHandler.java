package com.stratocloud.provider.aliyun.lb.classic.listener.udp;

import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListener;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerHandler;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AliyunUdpListenerHandler extends AliyunListenerHandler {

    public AliyunUdpListenerHandler(AliyunCloudProvider provider) {
        super(provider);
    }

    @Override
    protected boolean listenerFilter(AliyunListener listener) {
        String protocol = listener.detail().getListenerProtocol();
        return Objects.equals("udp", protocol);
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_UDP_LISTENER";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云UDP监听器";
    }
}
