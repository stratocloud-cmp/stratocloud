package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.ims.v2.ImsClient;
import com.huaweicloud.sdk.ims.v2.model.ImageInfo;
import com.huaweicloud.sdk.ims.v2.model.ListImagesRequest;
import com.huaweicloud.sdk.ims.v2.region.ImsRegion;
import com.stratocloud.cache.CacheService;

import java.util.List;
import java.util.Optional;

public class HuaweiImsServiceImpl extends HuaweiAbstractService implements HuaweiImsService{

    public HuaweiImsServiceImpl(CacheService cacheService,
                                ICredential credential,
                                String regionId,
                                String accessKeyId) {
        super(cacheService, credential, regionId, accessKeyId);
    }

    private ImsClient buildClient(){
        return ImsClient.newBuilder()
                .withCredential(credential)
                .withRegion(ImsRegion.valueOf(regionId))
                .build();
    }

    @Override
    public Optional<ImageInfo> describeImage(String imageId) {
        return describeImages(new ListImagesRequest().withId(imageId)).stream().findAny();
    }

    @Override
    public List<ImageInfo> describeImages(ListImagesRequest request){
        return queryAll(
                () -> buildClient().listImages(request).getImages(),
                request::setLimit,
                request::setMarker,
                ImageInfo::getId
        );
    }
}
