package com.stratocloud.provider.aliyun.common.services;

import com.aliyun.cms20190101.Client;
import com.aliyun.cms20190101.models.DescribeMetricLastRequest;
import com.aliyun.teaopenapi.models.Config;
import com.fasterxml.jackson.core.type.TypeReference;
import com.stratocloud.cache.CacheService;
import com.stratocloud.exceptions.ExternalAccountInvalidException;
import com.stratocloud.provider.aliyun.common.AliyunMetricDataPoint;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;

import java.util.List;

public class AliyunCmsServiceImpl extends AliyunAbstractService implements AliyunCmsService {
    public AliyunCmsServiceImpl(CacheService cacheService, Config config) {
        super(cacheService, config);
    }

    private Client buildClient(){
        try {
            Config newConfig = Config.build(config.toMap());
            newConfig.setEndpoint("metrics.%s.aliyuncs.com".formatted(config.getRegionId()));
            return new Client(newConfig);
        } catch (Exception e) {
            throw new ExternalAccountInvalidException(e.getMessage(), e);
        }
    }

    @Override
    public List<AliyunMetricDataPoint> describeLatestMetrics(DescribeMetricLastRequest request){
        request.setRegionId(config.getRegionId());
        var responseBody = tryInvoke(() -> buildClient().describeMetricLast(request)).getBody();

        String dataPointsStr = responseBody.getDatapoints();

        if(Utils.isBlank(dataPointsStr))
            return List.of();

        return JSON.toJavaList(dataPointsStr, new TypeReference<>(){});
    }
}
