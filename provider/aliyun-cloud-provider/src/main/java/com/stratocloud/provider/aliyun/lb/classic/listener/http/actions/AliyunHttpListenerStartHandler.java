package com.stratocloud.provider.aliyun.lb.classic.listener.http.actions;

import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerStartHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.http.AliyunHttpListenerHandler;
import org.springframework.stereotype.Component;

@Component
public class AliyunHttpListenerStartHandler extends AliyunListenerStartHandler {
    public AliyunHttpListenerStartHandler(AliyunHttpListenerHandler listenerHandler) {
        super(listenerHandler);
    }
}
