package com.stratocloud.provider.aliyun.lb.classic.listener.tcp;

import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListener;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerHandler;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AliyunTcpListenerHandler extends AliyunListenerHandler {

    public AliyunTcpListenerHandler(AliyunCloudProvider provider) {
        super(provider);
    }

    @Override
    protected boolean listenerFilter(AliyunListener listener) {
        String protocol = listener.detail().getListenerProtocol();
        return Objects.equals("tcp", protocol);
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_TCP_LISTENER";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云TCP监听器";
    }
}
