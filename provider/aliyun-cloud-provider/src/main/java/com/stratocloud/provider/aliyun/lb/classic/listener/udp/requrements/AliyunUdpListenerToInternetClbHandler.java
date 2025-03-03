package com.stratocloud.provider.aliyun.lb.classic.listener.udp.requrements;

import com.stratocloud.provider.aliyun.lb.classic.AliyunInternetClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerToClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.udp.AliyunUdpListenerHandler;
import org.springframework.stereotype.Component;

@Component
public class AliyunUdpListenerToInternetClbHandler extends AliyunListenerToClbHandler {

    public static final String TYPE_ID = "ALIYUN_UDP_LISTENER_TO_INTERNET_CLB_RELATIONSHIP";

    public AliyunUdpListenerToInternetClbHandler(AliyunUdpListenerHandler listenerHandler,
                                                 AliyunInternetClbHandler clbHandler) {
        super(listenerHandler, clbHandler);
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "阿里云UDP监听器与公网CLB";
    }

    @Override
    public String getCapabilityName() {
        return "UDP监听器";
    }

    @Override
    public String getRequirementName() {
        return "公网CLB";
    }
}
