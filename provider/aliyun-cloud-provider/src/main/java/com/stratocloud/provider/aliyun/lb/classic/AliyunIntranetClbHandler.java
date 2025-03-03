package com.stratocloud.provider.aliyun.lb.classic;

import com.stratocloud.provider.ResourcePropertiesUtil;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class AliyunIntranetClbHandler extends AliyunClbHandler {

    public AliyunIntranetClbHandler(AliyunCloudProvider provider) {
        super(provider);
    }

    @Override
    protected boolean filterLb(AliyunClb aliyunClb) {
        return Objects.equals("intranet", aliyunClb.detail().getAddressType());
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_INTRANET_CLB";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云内网CLB";
    }

    @Override
    public Map<String, Object> getPropertiesAtIndex(Map<String, Object> properties, int index) {
        return ResourcePropertiesUtil.getPropertiesAtIndex(properties, index, List.of("ipAddress"));
    }
}
