package com.stratocloud.provider.aliyun.lb.classic.listener.http.actions;

import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerDestroyHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.http.AliyunHttpListenerHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.http.requrements.AliyunHttpListenerToInternetClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.http.requrements.AliyunHttpListenerToIntranetClbHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AliyunHttpListenerDestroyHandler extends AliyunListenerDestroyHandler {
    public AliyunHttpListenerDestroyHandler(AliyunHttpListenerHandler listenerHandler) {
        super(listenerHandler);
    }

    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(
                AliyunHttpListenerToIntranetClbHandler.TYPE_ID,
                AliyunHttpListenerToInternetClbHandler.TYPE_ID
        );
    }
}
