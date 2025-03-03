package com.stratocloud.provider.aliyun.lb.classic.listener.http;

import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListener;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerHandler;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AliyunHttpListenerHandler extends AliyunListenerHandler {

    public AliyunHttpListenerHandler(AliyunCloudProvider provider) {
        super(provider);
    }

    @Override
    protected boolean listenerFilter(AliyunListener listener) {
        String protocol = listener.detail().getListenerProtocol();
        return Objects.equals("http", protocol);
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_HTTP_LISTENER";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云HTTP监听器";
    }
}
