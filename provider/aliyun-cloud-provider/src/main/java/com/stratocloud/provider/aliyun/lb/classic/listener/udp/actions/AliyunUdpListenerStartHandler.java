package com.stratocloud.provider.aliyun.lb.classic.listener.udp.actions;

import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerStartHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.udp.AliyunUdpListenerHandler;
import org.springframework.stereotype.Component;

@Component
public class AliyunUdpListenerStartHandler extends AliyunListenerStartHandler {
    public AliyunUdpListenerStartHandler(AliyunUdpListenerHandler listenerHandler) {
        super(listenerHandler);
    }
}
