package com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.requirements;

import com.stratocloud.provider.aliyun.lb.classic.AliyunInternetClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.AliyunClbEcsBackendHandler;
import org.springframework.stereotype.Component;

@Component
public class AliyunEcsBackendToInternetClbHandler extends AliyunBackendToClbHandler {

    public AliyunEcsBackendToInternetClbHandler(AliyunClbEcsBackendHandler backendHandler,
                                                AliyunInternetClbHandler clbHandler) {
        super(backendHandler, clbHandler);
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_ECS_BACKEND_TO_INTERNET_CLB_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "公网CLB与ECS后端服务";
    }

    @Override
    public String getCapabilityName() {
        return "ECS后端服务";
    }

    @Override
    public String getRequirementName() {
        return "公网CLB";
    }
}
