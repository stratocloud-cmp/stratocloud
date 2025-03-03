package com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.actions;

import com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.AliyunClbEcsBackendHandler;
import org.springframework.stereotype.Component;

@Component
public class AliyunClbEcsBackendDestroyHandler extends AliyunClbBackendDestroyHandler{
    public AliyunClbEcsBackendDestroyHandler(AliyunClbEcsBackendHandler backendHandler) {
        super(backendHandler);
    }
}
