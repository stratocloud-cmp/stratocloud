package com.stratocloud.provider.aliyun.lb.classic.listener.udp.actions;

import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerDestroyHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.udp.AliyunUdpListenerHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.udp.requrements.AliyunUdpListenerToInternetClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.udp.requrements.AliyunUdpListenerToIntranetClbHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AliyunUdpListenerDestroyHandler extends AliyunListenerDestroyHandler {
    public AliyunUdpListenerDestroyHandler(AliyunUdpListenerHandler listenerHandler) {
        super(listenerHandler);
    }

    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(
                AliyunUdpListenerToInternetClbHandler.TYPE_ID,
                AliyunUdpListenerToIntranetClbHandler.TYPE_ID
        );
    }
}
