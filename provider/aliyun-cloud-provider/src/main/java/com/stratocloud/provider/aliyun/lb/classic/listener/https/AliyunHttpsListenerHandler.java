package com.stratocloud.provider.aliyun.lb.classic.listener.https;

import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListener;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerHandler;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AliyunHttpsListenerHandler extends AliyunListenerHandler {

    public AliyunHttpsListenerHandler(AliyunCloudProvider provider) {
        super(provider);
    }

    @Override
    protected boolean listenerFilter(AliyunListener listener) {
        String protocol = listener.detail().getListenerProtocol();
        return Objects.equals("https", protocol);
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_HTTPS_LISTENER";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云HTTPS监听器";
    }
}
