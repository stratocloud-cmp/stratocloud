package com.stratocloud.provider.aliyun.lb.classic.listener.http.requrements;

import com.stratocloud.provider.aliyun.lb.classic.AliyunInternetClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerToClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.http.AliyunHttpListenerHandler;
import org.springframework.stereotype.Component;

@Component
public class AliyunHttpListenerToInternetClbHandler extends AliyunListenerToClbHandler {

    public static final String TYPE_ID = "ALIYUN_HTTP_LISTENER_TO_INTERNET_CLB_RELATIONSHIP";

    public AliyunHttpListenerToInternetClbHandler(AliyunHttpListenerHandler listenerHandler,
                                                  AliyunInternetClbHandler clbHandler) {
        super(listenerHandler, clbHandler);
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "阿里云HTTP监听器与公网CLB";
    }

    @Override
    public String getCapabilityName() {
        return "HTTP监听器";
    }

    @Override
    public String getRequirementName() {
        return "公网CLB";
    }
}
