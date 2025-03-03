package com.stratocloud.provider.aliyun.lb.classic.listener.tcp.actions;

import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerStopHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.tcp.AliyunTcpListenerHandler;
import org.springframework.stereotype.Component;

@Component
public class AliyunTcpListenerStopHandler extends AliyunListenerStopHandler {
    public AliyunTcpListenerStopHandler(AliyunTcpListenerHandler listenerHandler) {
        super(listenerHandler);
    }
}
