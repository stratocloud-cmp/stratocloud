package com.stratocloud.provider.aliyun.lb.classic;

import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AliyunInternetClbHandler extends AliyunClbHandler {

    public AliyunInternetClbHandler(AliyunCloudProvider provider) {
        super(provider);
    }

    @Override
    protected boolean filterLb(AliyunClb aliyunClb) {
        return Objects.equals("internet", aliyunClb.detail().getAddressType());
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_INTERNET_CLB";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云公网CLB";
    }
}
