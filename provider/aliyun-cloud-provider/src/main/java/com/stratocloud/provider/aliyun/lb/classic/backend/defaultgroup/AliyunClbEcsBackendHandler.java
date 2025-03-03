package com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup;

import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AliyunClbEcsBackendHandler extends AliyunClbBackendHandler {

    public AliyunClbEcsBackendHandler(AliyunCloudProvider provider) {
        super(provider);
    }

    @Override
    protected boolean filterBackend(AliyunClbBackend backend) {
        return Objects.equals(backend.id().resourceType(), "ecs");
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_CLB_ECS_BACKEND";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云ECS后端服务";
    }
}
