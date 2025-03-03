package com.stratocloud.provider.aliyun.lb.classic.listener.https.actions;

import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerDestroyHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.https.AliyunHttpsListenerHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.https.requrements.AliyunHttpsListenerToInternetClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.https.requrements.AliyunHttpsListenerToIntranetClbHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AliyunHttpsListenerDestroyHandler extends AliyunListenerDestroyHandler {
    public AliyunHttpsListenerDestroyHandler(AliyunHttpsListenerHandler listenerHandler) {
        super(listenerHandler);
    }

    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(
                AliyunHttpsListenerToIntranetClbHandler.TYPE_ID,
                AliyunHttpsListenerToInternetClbHandler.TYPE_ID
        );
    }
}
