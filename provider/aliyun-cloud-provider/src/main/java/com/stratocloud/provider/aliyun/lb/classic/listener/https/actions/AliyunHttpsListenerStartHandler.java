package com.stratocloud.provider.aliyun.lb.classic.listener.https.actions;

import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerStartHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.https.AliyunHttpsListenerHandler;
import org.springframework.stereotype.Component;

@Component
public class AliyunHttpsListenerStartHandler extends AliyunListenerStartHandler {
    public AliyunHttpsListenerStartHandler(AliyunHttpsListenerHandler listenerHandler) {
        super(listenerHandler);
    }
}
