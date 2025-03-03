package com.stratocloud.provider.aliyun.lb.classic.listener.tcp.actions;

import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerDestroyHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.tcp.AliyunTcpListenerHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.tcp.requrements.AliyunTcpListenerToInternetClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.tcp.requrements.AliyunTcpListenerToIntranetClbHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AliyunTcpListenerDestroyHandler extends AliyunListenerDestroyHandler {
    public AliyunTcpListenerDestroyHandler(AliyunTcpListenerHandler listenerHandler) {
        super(listenerHandler);
    }

    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(
                AliyunTcpListenerToIntranetClbHandler.TYPE_ID,
                AliyunTcpListenerToInternetClbHandler.TYPE_ID
        );
    }
}
