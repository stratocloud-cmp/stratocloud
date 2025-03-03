package com.stratocloud.provider.aliyun.lb.classic.listener.http.requrements;

import com.stratocloud.provider.aliyun.lb.classic.AliyunIntranetClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerToClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.http.AliyunHttpListenerHandler;
import org.springframework.stereotype.Component;

@Component
public class AliyunHttpListenerToIntranetClbHandler extends AliyunListenerToClbHandler {

    public static final String TYPE_ID = "ALIYUN_HTTP_LISTENER_TO_INTRANET_CLB_RELATIONSHIP";

    public AliyunHttpListenerToIntranetClbHandler(AliyunHttpListenerHandler listenerHandler,
                                                  AliyunIntranetClbHandler clbHandler) {
        super(listenerHandler, clbHandler);
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "阿里云HTTP监听器与内网CLB";
    }

    @Override
    public String getCapabilityName() {
        return "HTTP监听器";
    }

    @Override
    public String getRequirementName() {
        return "内网CLB";
    }
}
