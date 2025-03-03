package com.stratocloud.provider.aliyun.lb.classic.listener.tcp.requrements;

import com.stratocloud.provider.aliyun.lb.classic.AliyunIntranetClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerToClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.tcp.AliyunTcpListenerHandler;
import org.springframework.stereotype.Component;

@Component
public class AliyunTcpListenerToIntranetClbHandler extends AliyunListenerToClbHandler {

    public static final String TYPE_ID = "ALIYUN_TCP_LISTENER_TO_INTRANET_CLB_RELATIONSHIP";

    public AliyunTcpListenerToIntranetClbHandler(AliyunTcpListenerHandler listenerHandler,
                                                 AliyunIntranetClbHandler clbHandler) {
        super(listenerHandler, clbHandler);
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "阿里云TCP监听器与内网CLB";
    }

    @Override
    public String getCapabilityName() {
        return "TCP监听器";
    }

    @Override
    public String getRequirementName() {
        return "内网CLB";
    }
}
