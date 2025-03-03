package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.ecs.v2.EcsClient;
import com.huaweicloud.sdk.ecs.v2.model.*;
import com.huaweicloud.sdk.ecs.v2.region.EcsRegion;
import com.huaweicloud.sdk.ims.v2.model.ImageInfo;
import com.stratocloud.cache.CacheService;
import com.stratocloud.cache.CacheUtil;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.utils.Assert;
import com.stratocloud.utils.SecurityUtil;
import com.stratocloud.utils.Utils;
import com.stratocloud.utils.concurrent.SleepUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class HuaweiEcsServiceImpl extends HuaweiAbstractService implements HuaweiEcsService {

    public HuaweiEcsServiceImpl(CacheService cacheService,
                                ICredential credential,
                                String regionId,
                                String accessKeyId) {
        super(cacheService, credential, regionId, accessKeyId);
    }

    private EcsClient buildClient(){
        return EcsClient.newBuilder()
                .withCredential(credential)
                .withRegion(EcsRegion.valueOf(regionId))
                .build();
    }

    @Override
    public List<NovaAvailabilityZone> describeZones(){
        return CacheUtil.queryWithCache(
                cacheService,
                buildCacheKey("Zones"),
                300L,
                this::doDescribeZones,
                new ArrayList<>()
        );
    }

    private List<NovaAvailabilityZone> doDescribeZones() {
        var request = new NovaListAvailabilityZonesRequest();
        return queryAll(
                () -> buildClient().novaListAvailabilityZones(request).getAvailabilityZoneInfo()
        );
    }

    @Override
    public Optional<NovaAvailabilityZone> describeZone(String zoneId){
        return describeZones().stream().filter(
                z -> Objects.equals(zoneId, z.getZoneName())
        ).findAny();
    }

    @Override
    public List<ServerDetail> describeServers(ListServersDetailsRequest request) {
        return queryAll(
                () -> buildClient().listServersDetails(request).getServers(),
                request::setLimit,
                request::setMarker,
                ServerDetail::getId
        );
    }

    @Override
    public Optional<ServerDetail> describeServer(String serverId) {
        Optional<ServerDetail> result = describeServers(
                new ListServersDetailsRequest().withServerId(serverId)
        ).stream().findAny();

        if(result.isPresent() && Objects.equals("DELETED", result.get().getStatus()))
            return Optional.empty();

        return result;
    }


    @Override
    public void attachPort(String serverId, String portId) {
        tryInvoke(
                () -> buildClient().novaAttachInterface(
                        new NovaAttachInterfaceRequest().withServerId(serverId).withBody(
                                new NovaAttachInterfaceRequestBody().withInterfaceAttachment(
                                        new NovaAttachInterfaceOption().withPortId(portId)
                                )
                        )
                )
        );

        log.info("Huawei attach port request sent. ServerId={}. PortId={}.",
                serverId, portId);
    }



    @Override
    public List<InterfaceAttachment> listServerInterfaces(String serverId) {
        Optional<ServerDetail> serverDetail = describeServer(serverId);

        if(serverDetail.isEmpty())
            return List.of();

        return queryAll(
                () -> buildClient().listServerInterfaces(
                        new ListServerInterfacesRequest().withServerId(serverId)
                ).getInterfaceAttachments()
        );
    }

    @Override
    public void detachPort(String serverId, String portId) {
        tryInvoke(
                () -> buildClient().batchDeleteServerNics(
                        new BatchDeleteServerNicsRequest().withServerId(serverId).withBody(
                                new BatchDeleteServerNicsRequestBody().addNicsItem(
                                        new BatchDeleteServerNicOption().withId(portId)
                                )
                        )
                )
        );
        log.info("Huawei detach port request sent. ServerId={}. PortId={}.",
                serverId, portId);
    }

    @Override
    public Optional<Flavor> describeFlavor(String flavorId) {
        return describeFlavors().stream().filter(
                flavor -> flavor.getId().equals(flavorId)
        ).findAny();
    }


    @Override
    public List<Flavor> describeFlavors() {
        return CacheUtil.queryWithCache(
                cacheService,
                buildCacheKey("Flavor"),
                300L,
                this::doDescribeFlavors,
                new ArrayList<>()
        );
    }

    private List<Flavor> doDescribeFlavors() {
        return queryAll(
                () -> buildClient().listFlavors(new ListFlavorsRequest()).getFlavors()
        );
    }

    @Override
    public void resizeServer(String serverId, String flavorId) {
        String jobId = tryInvoke(() -> buildClient().resizeServer(
                new ResizeServerRequest().withServerId(serverId).withBody(
                        new ResizeServerRequestBody().withResize(
                                new ResizePrePaidServerOption().withFlavorRef(flavorId)
                        )
                )
        )).getJobId();

        log.info("Huawei resize server request sent. JobId={}. ServerId={}. FlavorId={}.",
                jobId, serverId, flavorId);

        waitForJob(jobId);

        log.info("Huawei resize server request succeeded. JobId={}. ServerId={}. FlavorId={}.",
                jobId, serverId, flavorId);
    }

    @Override
    public ShowJobResponse describeEcsJob(String jobId){
        return tryInvoke(
                () -> buildClient().showJob(new ShowJobRequest().withJobId(jobId))
        );
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

            while (waitingStatusSet.contains(currentStatus=describeEcsJob(jobId).getStatus()) && tried<maxTries){
                tried++;
                log.warn("Waiting ecs job to finish. CurrentStatus={}.({}/{})",
                        currentStatus, tried, maxTries);
                SleepUtil.sleep(10);
            }
        }catch (Exception e){
            log.warn(e.toString());
        }

        if(!Objects.equals(currentStatus, ShowJobResponse.StatusEnum.SUCCESS))
            throw new StratoException("Ecs job is in status %s.".formatted(currentStatus));
    }


    @Override
    public void rebuildServer(ServerDetail server, ImageInfo targetImage, String adminPass, String userData) {
        boolean changingOs = Objects.equals(server.getImage().getId(), targetImage.getId());
        boolean supportCloudInit = Objects.equals(targetImage.getSupportFcInject(), ImageInfo.SupportFcInjectEnum.TRUE);

        String encodedUserData = Utils.isBlank(userData) ? null : SecurityUtil.encodeToBase64(userData);

        String jobId;
        if(changingOs){
            jobId = changeServerOs(server, targetImage, adminPass, supportCloudInit, encodedUserData);
        }else {
            jobId = reinstallServer(server, adminPass, supportCloudInit, encodedUserData);
        }

        log.info("Huawei rebuild server request sent. JobId={}. ChangingOs={}. SupportCloudInit={}.",
                jobId, changingOs, supportCloudInit);

        waitForJob(jobId);
    }

    private String reinstallServer(ServerDetail server,
                                   String adminPass,
                                   boolean supportCloudInit,
                                   String encodedUserData) {
        if(supportCloudInit){
            ReinstallServerWithCloudInitOption option = new ReinstallServerWithCloudInitOption();
            option.withAdminpass(adminPass);
            if(Utils.isNotBlank(encodedUserData))
                option.withMetadata(new ReinstallSeverMetadata().withUserData(encodedUserData));

            return tryInvoke(
                    () -> buildClient().reinstallServerWithCloudInit(
                            new ReinstallServerWithCloudInitRequest().withServerId(server.getId()).withBody(
                                    new ReinstallServerWithCloudInitRequestBody().withOsReinstall(
                                            new ReinstallServerWithCloudInitOption().withAdminpass(adminPass)
                                    )
                            )
                    )
            ).getJobId();
        }else{
            ReinstallServerWithoutCloudInitOption option = new ReinstallServerWithoutCloudInitOption();
            option.withAdminpass(adminPass);

            return tryInvoke(
                    () -> buildClient().reinstallServerWithoutCloudInit(
                            new ReinstallServerWithoutCloudInitRequest().withServerId(server.getId()).withBody(
                                    new ReinstallServerWithoutCloudInitRequestBody().withOsReinstall(
                                            new ReinstallServerWithoutCloudInitOption().withAdminpass(adminPass)
                                    )
                            )
                    )
            ).getJobId();
        }
    }

    private String changeServerOs(ServerDetail server,
                                  ImageInfo targetImage,
                                  String adminPass,
                                  boolean supportCloudInit,
                                  String encodedUserData) {
        if(supportCloudInit){
            ChangeServerOsWithCloudInitOption option = new ChangeServerOsWithCloudInitOption();
            option.withImageid(targetImage.getId()).withAdminpass(adminPass);
            if(Utils.isNotBlank(encodedUserData))
                option.withMetadata(new ChangeSeversOsMetadata().withUserData(encodedUserData));

            return tryInvoke(
                    () -> buildClient().changeServerOsWithCloudInit(
                            new ChangeServerOsWithCloudInitRequest()
                                    .withServerId(server.getId())
                                    .withBody(
                                            new ChangeServerOsWithCloudInitRequestBody().withOsChange(option)
                                    )
                    )
            ).getJobId();
        }else {
            ChangeServerOsWithoutCloudInitOption option = new ChangeServerOsWithoutCloudInitOption();
            option.withImageid(targetImage.getId()).withAdminpass(adminPass);

            return tryInvoke(
                    () -> buildClient().changeServerOsWithoutCloudInit(
                            new ChangeServerOsWithoutCloudInitRequest()
                                    .withServerId(server.getId())
                                    .withBody(
                                            new ChangeServerOsWithoutCloudInitRequestBody().withOsChange(option)
                                    )
                    )
            ).getJobId();
        }
    }

    @Override
    public Optional<ServerBlockDevice> describeServerSystemDisk(String serverId) {
        return queryAll(
                () -> buildClient().listServerBlockDevices(
                        new ListServerBlockDevicesRequest().withServerId(serverId)
                ).getVolumeAttachments()
        ).stream().filter(b -> b.getBootIndex() == 0).findAny();
    }


    @Override
    public String createPrePaidServer(CreateServersRequest request) {
        Integer count = request.getBody().getServer().getCount();
        Assert.isTrue(count==null||count==1, "Do not create multiple server via this method.");

        boolean dryRun = request.getBody().getDryRun() != null ? request.getBody().getDryRun() : false;
        List<String> serverIds = tryInvoke(() -> buildClient().createServers(request)).getServerIds();

        if (dryRun) {
            log.info("Huawei dry run create pre-paid server request accepted.");
            return null;
        } else {
            String serverId = serverIds.get(0);
            log.info("Huawei create pre-paid server request sent. ServerId={}.", serverId);
            return serverId;
        }
    }

    @Override
    public String createPostPaidServer(CreatePostPaidServersRequest request) {
        Integer count = request.getBody().getServer().getCount();
        Assert.isTrue(count==null||count==1, "Do not create multiple server via this method.");

        boolean dryRun = request.getBody().getDryRun() != null ? request.getBody().getDryRun() : false;
        List<String> serverIds = tryInvoke(() -> buildClient().createPostPaidServers(request)).getServerIds();

        if (dryRun) {
            log.info("Huawei dry run create post-paid server request accepted.");
            return null;
        } else {
            String serverId = serverIds.get(0);
            log.info("Huawei create post-paid server request sent. ServerId={}.", serverId);
            return serverId;
        }
    }

    @Override
    public void deleteServer(String serverId) {
        String jobId = tryInvoke(
                () -> buildClient().deleteServers(
                        new DeleteServersRequest().withBody(
                                new DeleteServersRequestBody().addServersItem(
                                        new ServerId().withId(serverId)
                                )
                        )
                )
        ).getJobId();

        log.info("Huawei delete server request sent. JobId={}. ServerId={}.",
                jobId, serverId);

        waitForJob(jobId);
    }

    @Override
    public void rebootServerAndWait(String serverId, boolean hardReboot) {
        var rebootType = hardReboot ? BatchRebootSeversOption.TypeEnum.HARD : BatchRebootSeversOption.TypeEnum.SOFT;
        String jobId = tryInvoke(
                () -> buildClient().batchRebootServers(
                        new BatchRebootServersRequest().withBody(
                                new BatchRebootServersRequestBody().withReboot(
                                        new BatchRebootSeversOption().addServersItem(
                                                new ServerId().withId(serverId)
                                        ).withType(rebootType)
                                )
                        )
                )
        ).getJobId();

        log.info("Huawei reboot server request sent. JobId={}. ServerId={}.",
                jobId, serverId);

        waitForJob(jobId);
    }

    @Override
    public void startServerAndWait(String serverId) {
        String jobId = tryInvoke(
                () -> buildClient().batchStartServers(
                        new BatchStartServersRequest().withBody(
                                new BatchStartServersRequestBody().withOsStart(
                                        new BatchStartServersOption().addServersItem(
                                                new ServerId().withId(serverId)
                                        )
                                )
                        )
                )
        ).getJobId();

        log.info("Huawei start server request sent. JobId={}. ServerId={}.",
                jobId, serverId);

        waitForJob(jobId);
    }

    @Override
    public void stopServerAndWait(String serverId, boolean hardStop) {
        var stopType = hardStop ? BatchStopServersOption.TypeEnum.HARD : BatchStopServersOption.TypeEnum.SOFT;
        String jobId = tryInvoke(
                () -> buildClient().batchStopServers(
                        new BatchStopServersRequest().withBody(
                                new BatchStopServersRequestBody().withOsStop(
                                        new BatchStopServersOption().addServersItem(
                                                new ServerId().withId(serverId)
                                        ).withType(stopType)
                                )
                        )
                )
        ).getJobId();

        log.info("Huawei stop server request sent. JobId={}. ServerId={}.",
                jobId, serverId);

        waitForJob(jobId);
    }

    @Override
    public String describeServerConsoleUrl(String serverId) {
        return tryInvoke(
                () -> buildClient().showServerRemoteConsole(
                        new ShowServerRemoteConsoleRequest().withServerId(serverId).withBody(
                                new ShowServerRemoteConsoleRequestBody().withRemoteConsole(
                                        new GetServerRemoteConsoleOption().withProtocol(
                                                GetServerRemoteConsoleOption.ProtocolEnum.VNC
                                        ).withType(
                                                GetServerRemoteConsoleOption.TypeEnum.NOVNC
                                        )
                                )
                        )
                )
        ).getRemoteConsole().getUrl();
    }

    @Override
    public void attachVolume(String serverId, String volumeId) {
        String jobId = tryInvoke(
                () -> buildClient().attachServerVolume(
                        new AttachServerVolumeRequest().withServerId(serverId).withBody(
                                new AttachServerVolumeRequestBody().withVolumeAttachment(
                                        new AttachServerVolumeOption().withVolumeId(volumeId)
                                )
                        )
                )
        ).getJobId();

        log.info("Huawei attach volume request sent. JobId={}. ServerId={}. VolumeId={}.",
                jobId, serverId, volumeId);

        waitForJob(jobId);
    }

    @Override
    public void detachVolume(String serverId, String volumeId) {
        String jobId = tryInvoke(
                () -> buildClient().detachServerVolume(
                        new DetachServerVolumeRequest().withServerId(serverId).withVolumeId(volumeId)
                )
        ).getJobId();

        log.info("Huawei detach volume request sent. JobId={}. ServerId={}. VolumeId={}.",
                jobId, serverId, volumeId);

        waitForJob(jobId);
    }

    @Override
    public void resetServerPassword(ResetServerPasswordRequest request) {
        tryInvoke(
                () -> buildClient().resetServerPassword(request)
        );

        log.info("Huawei reset server password request sent. ServerId={}.", request.getServerId());
    }

    @Override
    public boolean isResetPasswordSupported(String serverId) {
        String flag = tryInvoke(
                () -> buildClient().showResetPasswordFlag(
                        new ShowResetPasswordFlagRequest().withServerId(serverId)
                )
        ).getResetpwdFlag();

        return Objects.equals(flag, "True");
    }

    @Override
    public void updateServer(UpdateServerRequest request){
        tryInvoke(
                () -> buildClient().updateServer(request)
        );

        log.info("Huawei update server request sent. ServerId={}.", request.getServerId());
    }
}
