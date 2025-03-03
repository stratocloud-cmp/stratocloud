package com.stratocloud.provider.aliyun.lb.classic.listener.udp.actions;

import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerStopHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.udp.AliyunUdpListenerHandler;
import org.springframework.stereotype.Component;

@Component
public class AliyunUdpListenerStopHandler extends AliyunListenerStopHandler {
    public AliyunUdpListenerStopHandler(AliyunUdpListenerHandler listenerHandler) {
        super(listenerHandler);
    }
}
