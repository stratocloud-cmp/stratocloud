package com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.requirements;

import com.stratocloud.provider.aliyun.lb.classic.AliyunIntranetClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.AliyunClbEniBackendHandler;
import org.springframework.stereotype.Component;

@Component
public class AliyunEniBackendToIntranetClbHandler extends AliyunBackendToClbHandler {

    public AliyunEniBackendToIntranetClbHandler(AliyunClbEniBackendHandler backendHandler,
                                                AliyunIntranetClbHandler clbHandler) {
        super(backendHandler, clbHandler);
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_ENI_BACKEND_TO_INTRANET_CLB_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "内网CLB与ENI后端服务";
    }

    @Override
    public String getCapabilityName() {
        return "ENI后端服务";
    }

    @Override
    public String getRequirementName() {
        return "内网CLB";
    }
}
