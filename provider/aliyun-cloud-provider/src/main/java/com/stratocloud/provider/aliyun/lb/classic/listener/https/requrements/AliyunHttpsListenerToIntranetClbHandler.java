package com.stratocloud.provider.aliyun.lb.classic.listener.https.requrements;

import com.stratocloud.provider.aliyun.lb.classic.AliyunIntranetClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerToClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.https.AliyunHttpsListenerHandler;
import org.springframework.stereotype.Component;

@Component
public class AliyunHttpsListenerToIntranetClbHandler extends AliyunListenerToClbHandler {

    public static final String TYPE_ID = "ALIYUN_HTTPS_LISTENER_TO_INTRANET_CLB_RELATIONSHIP";

    public AliyunHttpsListenerToIntranetClbHandler(AliyunHttpsListenerHandler listenerHandler,
                                                   AliyunIntranetClbHandler clbHandler) {
        super(listenerHandler, clbHandler);
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "阿里云HTTPS监听器与内网CLB";
    }

    @Override
    public String getCapabilityName() {
        return "HTTPS监听器";
    }

    @Override
    public String getRequirementName() {
        return "内网CLB";
    }
}
