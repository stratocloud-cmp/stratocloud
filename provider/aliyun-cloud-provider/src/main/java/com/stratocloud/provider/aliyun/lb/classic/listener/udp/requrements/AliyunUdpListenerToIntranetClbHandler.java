package com.stratocloud.provider.aliyun.lb.classic.listener.udp.requrements;

import com.stratocloud.provider.aliyun.lb.classic.AliyunIntranetClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerToClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.udp.AliyunUdpListenerHandler;
import org.springframework.stereotype.Component;

@Component
public class AliyunUdpListenerToIntranetClbHandler extends AliyunListenerToClbHandler {

    public static final String TYPE_ID = "ALIYUN_UDP_LISTENER_TO_INTRANET_CLB_RELATIONSHIP";

    public AliyunUdpListenerToIntranetClbHandler(AliyunUdpListenerHandler listenerHandler,
                                                 AliyunIntranetClbHandler clbHandler) {
        super(listenerHandler, clbHandler);
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "阿里云UDP监听器与内网CLB";
    }

    @Override
    public String getCapabilityName() {
        return "UDP监听器";
    }

    @Override
    public String getRequirementName() {
        return "内网CLB";
    }
}
