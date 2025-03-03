package com.stratocloud.provider.aliyun.lb.classic.listener.tcp.actions;

import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerStartHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.tcp.AliyunTcpListenerHandler;
import org.springframework.stereotype.Component;

@Component
public class AliyunTcpListenerStartHandler extends AliyunListenerStartHandler {
    public AliyunTcpListenerStartHandler(AliyunTcpListenerHandler listenerHandler) {
        super(listenerHandler);
    }
}
