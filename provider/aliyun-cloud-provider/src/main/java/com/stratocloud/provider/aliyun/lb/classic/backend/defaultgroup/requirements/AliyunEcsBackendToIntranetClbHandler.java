package com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.requirements;

import com.stratocloud.provider.aliyun.lb.classic.AliyunIntranetClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.AliyunClbEcsBackendHandler;
import org.springframework.stereotype.Component;

@Component
public class AliyunEcsBackendToIntranetClbHandler extends AliyunBackendToClbHandler {

    public AliyunEcsBackendToIntranetClbHandler(AliyunClbEcsBackendHandler backendHandler,
                                                AliyunIntranetClbHandler clbHandler) {
        super(backendHandler, clbHandler);
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_ECS_BACKEND_TO_INTRANET_CLB_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "内网CLB与ECS后端服务";
    }

    @Override
    public String getCapabilityName() {
        return "ECS后端服务";
    }

    @Override
    public String getRequirementName() {
        return "内网CLB";
    }
}
