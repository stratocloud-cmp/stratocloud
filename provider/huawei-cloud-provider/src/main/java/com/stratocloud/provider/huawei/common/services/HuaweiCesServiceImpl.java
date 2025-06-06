package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.ces.v1.CesClient;
import com.huaweicloud.sdk.ces.v1.model.Datapoint;
import com.huaweicloud.sdk.ces.v1.model.ShowMetricDataRequest;
import com.huaweicloud.sdk.ces.v1.region.CesRegion;
import com.huaweicloud.sdk.core.auth.ICredential;
import com.stratocloud.cache.CacheService;

import java.util.List;

public class HuaweiCesServiceImpl extends HuaweiAbstractService implements HuaweiCesService {
    public HuaweiCesServiceImpl(CacheService cacheService, ICredential credential, String regionId, String accessKeyId) {
        super(cacheService, credential, regionId, accessKeyId);
    }

    private CesClient buildClient(){
        return CesClient.newBuilder()
                .withCredential(credential)
                .withRegion(CesRegion.valueOf(regionId))
                .build();
    }

    @Override
    public List<Datapoint> describeMetricData(ShowMetricDataRequest request){
        return queryAll(
                () -> buildClient().showMetricData(request).getDatapoints()
        );
    }
}
