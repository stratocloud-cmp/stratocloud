package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.evs.v2.EvsClient;
import com.huaweicloud.sdk.evs.v2.model.*;
import com.huaweicloud.sdk.evs.v2.region.EvsRegion;
import com.stratocloud.cache.CacheService;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.utils.concurrent.SleepUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class HuaweiEvsServiceImpl extends HuaweiAbstractService implements HuaweiEvsService {

    public HuaweiEvsServiceImpl(CacheService cacheService,
                                ICredential credential,
                                String regionId,
                                String accessKeyId) {
        super(cacheService, credential, regionId, accessKeyId);
    }

    private EvsClient buildClient(){
        return EvsClient.newBuilder()
                .withCredential(credential)
                .withRegion(EvsRegion.valueOf(regionId))
                .build();
    }

    @Override
    public List<VolumeDetail> describeVolumes(ListVolumesRequest request) {
        return queryAll(
                () -> buildClient().listVolumes(request).getVolumes(),
                request::setLimit,
                request::setMarker,
                VolumeDetail::getId
        );
    }

    @Override
    public Optional<VolumeDetail> describeVolume(String volumeId) {
        return queryOne(
                () -> buildClient().showVolume(
                        new ShowVolumeRequest().withVolumeId(volumeId)
                ).getVolume()
        );
    }

    @Override
    public String createVolume(CreateVolumeRequest request) {
        Integer count = request.getBody().getVolume().getCount();

        if(count != null && count > 1)
            throw new StratoException("Do not create multiple volumes via this method.");

        CreateVolumeResponse response = tryInvoke(
                () -> buildClient().createVolume(request)
        );

        String jobId = response.getJobId();
        String volumeId = response.getVolumeIds().get(0);

        log.info("Huawei create volume request sent. JobId={}. VolumeId={}.",
                jobId, volumeId);

        return volumeId;
    }

    @Override
    public void deleteVolume(String volumeId) {
        String jobId = tryInvoke(
                () -> buildClient().deleteVolume(
                        new DeleteVolumeRequest().withVolumeId(volumeId)
                )
        ).getJobId();

        log.info("Huawei delete volume request sent. JobId={}. VolumeId={}.",
                jobId, volumeId);

        waitForJob(jobId);
    }

    @Override
    public ShowJobResponse describeEvsJob(String jobId){
        return tryInvoke(
                () -> buildClient().showJob(new ShowJobRequest().withJobId(jobId))
        );
    }

    @Override
    public void resizeVolume(String volumeId, Integer newSize) {
        String jobId = tryInvoke(
                () -> buildClient().resizeVolume(
                        new ResizeVolumeRequest().withVolumeId(volumeId).withBody(
                                new ResizeVolumeRequestBody().withBssParam(
                                        new BssParamForResizeVolume().withIsAutoPay(
                                                BssParamForResizeVolume.IsAutoPayEnum.TRUE
                                        )
                                ).withOsExtend(new OsExtend().withNewSize(newSize))
                        )
                )
        ).getJobId();

        log.info("Huawei resize volume request sent. JobId={}. VolumeId={}.",
                jobId, volumeId);

        waitForJob(jobId);
    }

    @Override
    public void updateVolume(UpdateVolumeRequest request) {
        tryInvoke(
                () -> buildClient().updateVolume(request)
        );

        log.info("Huawei resize volume request sent. VolumeId={}.",
                request.getVolumeId());
    }

    private void waitForJob(String jobId){
        Set<ShowJobResponse.StatusEnum> waitingStatusSet = Set.of(
                ShowJobResponse.StatusEnum.INIT,
                ShowJobResponse.StatusEnum.RUNNING
        );

        ShowJobResponse.StatusEnum currentStatus = null;
        try {
            int tried = 0;
            int maxTries = 30;

            while (waitingStatusSet.contains(currentStatus= describeEvsJob(jobId).getStatus()) && tried<maxTries){
                tried++;
                log.warn("Waiting evs job to finish. CurrentStatus={}.({}/{})",
                        currentStatus, tried, maxTries);
                SleepUtil.sleep(10);
            }
        }catch (Exception e){
            log.warn(e.toString());
        }

        if(!Objects.equals(currentStatus, ShowJobResponse.StatusEnum.SUCCESS))
            throw new StratoException("Evs job is in status %s.".formatted(currentStatus));
    }

    @Override
    public Optional<SnapshotList> describeSnapshot(String snapshotId) {
        return describeSnapshots(
                new ListSnapshotsRequest().withId(snapshotId)
        ).stream().findAny();
    }

    @Override
    public List<SnapshotList> describeSnapshots(ListSnapshotsRequest request){
        return queryAll(
                () -> buildClient().listSnapshots(request).getSnapshots(),
                request::setLimit,
                request::setOffset
        );
    }

    @Override
    public String createSnapshot(CreateSnapshotRequest request) {
        String snapshotId = tryInvoke(
                () -> buildClient().createSnapshot(request)
        ).getSnapshot().getId();

        log.info("Huawei create snapshot request sent. SnapshotId={}.", snapshotId);

        return snapshotId;
    }

    @Override
    public void deleteSnapshot(String snapshotId) {
        tryInvoke(
                () -> buildClient().deleteSnapshot(
                        new DeleteSnapshotRequest().withSnapshotId(snapshotId)
                )
        );

        log.info("Huawei delete snapshot request sent. SnapshotId={}.", snapshotId);
    }

    @Override
    public void rollbackToSnapshot(String volumeId, String snapshotId) {
        tryInvoke(
                () -> buildClient().rollbackSnapshot(
                        new RollbackSnapshotRequest().withSnapshotId(snapshotId).withBody(
                                new RollbackSnapshotRequestBody().withRollback(
                                        new RollbackSnapshotOption().withVolumeId(volumeId)
                                )
                        )
                )
        );

        log.info("Huawei rollback snapshot request sent. SnapshotId={}.", snapshotId);
    }
}
