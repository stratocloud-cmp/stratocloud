package com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.actions;

import com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.AliyunClbEniBackendHandler;
import org.springframework.stereotype.Component;

@Component
public class AliyunClbEniBackendDestroyHandler extends AliyunClbBackendDestroyHandler{
    public AliyunClbEniBackendDestroyHandler(AliyunClbEniBackendHandler backendHandler) {
        super(backendHandler);
    }
}
