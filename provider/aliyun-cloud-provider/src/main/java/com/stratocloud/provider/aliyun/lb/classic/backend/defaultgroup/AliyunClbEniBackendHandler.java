package com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup;

import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AliyunClbEniBackendHandler extends AliyunClbBackendHandler {

    public AliyunClbEniBackendHandler(AliyunCloudProvider provider) {
        super(provider);
    }

    @Override
    protected boolean filterBackend(AliyunClbBackend backend) {
        return Objects.equals(backend.id().resourceType(), "eni");
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_CLB_ENI_BACKEND";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云ENI后端服务";
    }
}
