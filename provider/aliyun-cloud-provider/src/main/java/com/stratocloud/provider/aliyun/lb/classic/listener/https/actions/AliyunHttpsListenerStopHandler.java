package com.stratocloud.provider.aliyun.lb.classic.listener.https.actions;

import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerStopHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.https.AliyunHttpsListenerHandler;
import org.springframework.stereotype.Component;

@Component
public class AliyunHttpsListenerStopHandler extends AliyunListenerStopHandler {
    public AliyunHttpsListenerStopHandler(AliyunHttpsListenerHandler listenerHandler) {
        super(listenerHandler);
    }
}
