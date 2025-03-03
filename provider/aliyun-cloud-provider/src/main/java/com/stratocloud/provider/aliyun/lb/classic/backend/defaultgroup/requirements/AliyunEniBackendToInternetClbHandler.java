package com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.requirements;

import com.stratocloud.provider.aliyun.lb.classic.AliyunInternetClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.AliyunClbEniBackendHandler;
import org.springframework.stereotype.Component;

@Component
public class AliyunEniBackendToInternetClbHandler extends AliyunBackendToClbHandler {

    public AliyunEniBackendToInternetClbHandler(AliyunClbEniBackendHandler backendHandler,
                                                AliyunInternetClbHandler clbHandler) {
        super(backendHandler, clbHandler);
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_ENI_BACKEND_TO_INTERNET_CLB_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "公网CLB与ENI后端服务";
    }

    @Override
    public String getCapabilityName() {
        return "ENI后端服务";
    }

    @Override
    public String getRequirementName() {
        return "公网CLB";
    }
}
