package com.stratocloud.provider.aliyun.lb.classic.listener.http.actions;

import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerStopHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.http.AliyunHttpListenerHandler;
import org.springframework.stereotype.Component;

@Component
public class AliyunHttpListenerStopHandler extends AliyunListenerStopHandler {
    public AliyunHttpListenerStopHandler(AliyunHttpListenerHandler listenerHandler) {
        super(listenerHandler);
    }
}
