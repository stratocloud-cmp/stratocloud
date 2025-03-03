package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.evs.v2.model.*;

import java.util.List;
import java.util.Optional;

public interface HuaweiEvsService {
    List<VolumeDetail> describeVolumes(ListVolumesRequest request);

    Optional<VolumeDetail> describeVolume(String volumeId);

    String createVolume(CreateVolumeRequest request);

    void deleteVolume(String volumeId);

    ShowJobResponse describeEvsJob(String jobId);

    void resizeVolume(String volumeId, Integer newSize);

    void updateVolume(UpdateVolumeRequest request);
}
