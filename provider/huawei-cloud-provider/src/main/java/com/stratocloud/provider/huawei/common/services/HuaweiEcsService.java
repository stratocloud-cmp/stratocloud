package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.ecs.v2.model.*;
import com.huaweicloud.sdk.ims.v2.model.ImageInfo;

import java.util.List;
import java.util.Optional;

public interface HuaweiEcsService {

    List<NovaAvailabilityZone> describeZones();

    Optional<NovaAvailabilityZone> describeZone(String zoneId);

    Optional<ServerDetail> describeServer(String serverId);

    List<ServerDetail> describeServers(ListServersDetailsRequest request);

    void attachPort(String serverId, String portId);

    List<InterfaceAttachment> listServerInterfaces(String serverId);

    void detachPort(String serverId, String portId);

    Optional<Flavor> describeFlavor(String flavorId);

    List<Flavor> describeFlavors();

    void resizeServer(String serverId, String flavorId);

    ShowJobResponse describeEcsJob(String jobId);

    void rebuildServer(ServerDetail server, ImageInfo targetImage, String adminPass, String userData);

    Optional<ServerBlockDevice> describeServerSystemDisk(String serverId);

    String createPrePaidServer(CreateServersRequest request);

    String createPostPaidServer(CreatePostPaidServersRequest request);

    void deleteServer(String serverId);

    void rebootServerAndWait(String serverId, boolean hardReboot);

    void startServerAndWait(String serverId);

    void stopServerAndWait(String serverId, boolean hardStop);

    String describeServerConsoleUrl(String serverId);

    void attachVolume(String serverId, String volumeId);

    void detachVolume(String serverId, String volumeId);

    void resetServerPassword(ResetServerPasswordRequest request);

    boolean isResetPasswordSupported(String serverId);

    void updateServer(UpdateServerRequest request);
}
