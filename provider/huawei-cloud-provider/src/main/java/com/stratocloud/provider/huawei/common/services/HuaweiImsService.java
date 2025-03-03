package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.ims.v2.model.ImageInfo;
import com.huaweicloud.sdk.ims.v2.model.ListImagesRequest;

import java.util.List;
import java.util.Optional;

public interface HuaweiImsService {
    Optional<ImageInfo> describeImage(String imageId);

    List<ImageInfo> describeImages(ListImagesRequest request);
}
