package com.stratocloud.provider.huawei.servers;

import com.huaweicloud.sdk.cts.v3.model.Traces;
import com.huaweicloud.sdk.ecs.v2.model.ListServersDetailsRequest;
import com.huaweicloud.sdk.ecs.v2.model.ServerAddress;
import com.huaweicloud.sdk.ecs.v2.model.ServerDetail;
import com.huaweicloud.sdk.ecs.v2.model.ServerFlavor;
import com.huaweicloud.sdk.ims.v2.model.ImageInfo;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.event.ExternalResourceEvent;
import com.stratocloud.event.StratoEventLevel;
import com.stratocloud.event.StratoEventSource;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.RuntimePropertiesUtil;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.guest.command.ProviderGuestCommandExecutorFactory;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.common.HuaweiEventTypes;
import com.stratocloud.provider.huawei.servers.command.HuaweiBatCommandExecutorFactory;
import com.stratocloud.provider.huawei.servers.command.HuaweiShellCommandExecutorFactory;
import com.stratocloud.provider.resource.event.EventAwareResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.TimeUtil;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class HuaweiServerHandler extends AbstractResourceHandler
        implements GuestOsHandler, EventAwareResourceHandler {

    private final HuaweiCloudProvider provider;

    private final HuaweiShellCommandExecutorFactory shellCommandExecutorFactory;

    private final HuaweiBatCommandExecutorFactory batCommandExecutorFactory;

    public HuaweiServerHandler(HuaweiCloudProvider provider,
                               HuaweiShellCommandExecutorFactory shellCommandExecutorFactory,
                               HuaweiBatCommandExecutorFactory batCommandExecutorFactory) {
        this.provider = provider;
        this.shellCommandExecutorFactory = shellCommandExecutorFactory;
        this.batCommandExecutorFactory = batCommandExecutorFactory;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_SERVER";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云云主机";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.COMPUTE_INSTANCE;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeServer(account, externalId).map(
                server -> toExternalResource(account, server)
        );
    }

    public Optional<ServerDetail> describeServer(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).ecs().describeServer(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, ServerDetail server) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                server.getId(),
                server.getName(),
                convertStatus(server.getStatus())
        );
    }

    private ResourceState convertStatus(String status) {
        if(status == null)
            return ResourceState.UNKNOWN;

        return switch (status){
            case "ACTIVE"-> ResourceState.STARTED;
            case "BUILD"-> ResourceState.BUILDING;
            case "REBUILD"-> ResourceState.REBUILDING;
            case "SUSPENDED"-> ResourceState.SUSPENDED;
            case "PAUSED"-> ResourceState.PAUSED;
            case "RESIZE", "VERIFY_RESIZE", "REVERT_RESIZE", "MIGRATING", "PASSWORD" -> ResourceState.CONFIGURING;
            case "REBOOT", "HARD_REBOOT" -> ResourceState.RESTARTING;
            case "DELETED"-> ResourceState.DESTROYED;
            case "ERROR"-> ResourceState.ERROR;
            case "STOPPED", "SHUTOFF" -> ResourceState.STOPPED;
            case "SHELVED", "SHELVED_OFFLOADED" -> ResourceState.SHELVED;
            default -> ResourceState.UNKNOWN;
        };
    }


    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        HuaweiCloudClient client = provider.buildClient(account);
        return client.ecs().describeServers(new ListServersDetailsRequest()).stream().map(
                server -> toExternalResource(account, server)
        ).toList();
    }


    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        ServerDetail server = describeServer(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Server not found.")
        );

        resource.updateByExternal(toExternalResource(account, server));


        String hostname = server.getOsEXTSRVATTRHostname();
        if(Utils.isNotBlank(hostname)){
            RuntimeProperty hostNameProperty = RuntimeProperty.ofDisplayInList(
                    "hostName", "操作系统主机名", hostname, hostname
            );

            resource.addOrUpdateRuntimeProperty(hostNameProperty);
        }

        Map<String, List<ServerAddress>> addresses = server.getAddresses();
        List<String> ips = new ArrayList<>();
        List<String> floatingIps = new ArrayList<>();
        if(Utils.isNotEmpty(addresses))
            for (var entry : addresses.entrySet())
                if(Utils.isNotEmpty(entry.getValue()))
                    for (ServerAddress address : entry.getValue()) {
                        ips.add(address.getAddr());
                        if(ServerAddress.OsEXTIPSTypeEnum.FLOATING.equals(address.getOsEXTIPSType()))
                            floatingIps.add(address.getAddr());
                    }

        if(Utils.isNotEmpty(ips)){
            String ipsInfo = String.join(",", ips);
            RuntimeProperty ipsProperty = RuntimeProperty.ofDisplayInList(
                    "ipAddresses", "IP地址", ipsInfo, ipsInfo
            );

            resource.addOrUpdateRuntimeProperty(ipsProperty);
        }


        ServerFlavor flavor = server.getFlavor();
        if(flavor != null){
            int cpuCores = Integer.parseInt(flavor.getVcpus());
            int memoryMB = Integer.parseInt(flavor.getRam());
            int memoryGB = memoryMB >> 10;

            String sizeInfo = "%sC%sG".formatted(cpuCores, memoryGB);
            RuntimeProperty sizeProperty = RuntimeProperty.ofDisplayInList(
                    "size", "规格大小", sizeInfo, sizeInfo
            );
            resource.addOrUpdateRuntimeProperty(sizeProperty);

            resource.updateUsageByType(UsageTypes.CPU_CORES, BigDecimal.valueOf(cpuCores));
            resource.updateUsageByType(UsageTypes.MEMORY_GB, BigDecimal.valueOf(memoryGB));
        }

        RuntimePropertiesUtil.autoSyncGuestManagementInfoQuietly(resource);

        if(!floatingIps.isEmpty())
            RuntimePropertiesUtil.setManagementIp(resource, floatingIps.get(0));
        else if(!ips.isEmpty())
            RuntimePropertiesUtil.setManagementIp(resource, ips.get(0));
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of(
                UsageTypes.CPU_CORES, UsageTypes.MEMORY_GB
        );
    }

    @Override
    public boolean supportCascadedDestruction() {
        return true;
    }

    @Override
    public OsType getOsType(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<ServerDetail> server = describeServer(account, resource.getExternalId());

        if(server.isEmpty())
            return OsType.Unknown;

        String imageId = server.get().getImage().getId();

        HuaweiCloudClient client = provider.buildClient(account);


        Optional<ImageInfo> image = client.ims().describeImage(imageId);

        if(image.isEmpty())
            return OsType.Unknown;

        ImageInfo.OsTypeEnum osType = image.get().getOsType();

        if(ImageInfo.OsTypeEnum.LINUX.equals(osType))
            return OsType.Linux;
        else if(ImageInfo.OsTypeEnum.WINDOWS.equals(osType))
            return OsType.Windows;
        else
            return OsType.Unknown;
    }

    @Override
    public List<ProviderGuestCommandExecutorFactory> getProviderCommandExecutorFactories(Resource resource) {
        return List.of(
                shellCommandExecutorFactory,
                batCommandExecutorFactory
        );
    }

    @Override
    public ResourceCost getCurrentCost(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudClient client = provider.buildClient(account);

        Optional<ServerDetail> serverDetail = describeServer(account, resource.getExternalId());

        if(serverDetail.isEmpty())
            return ResourceCost.ZERO;

        Map<String, String> metadata = serverDetail.get().getMetadata();

        if(Utils.isEmpty(metadata))
            return ResourceCost.ZERO;

        String chargingMode = serverDetail.get().getMetadata().get("charging_mode");

        String osSuffix = getOsType(resource) == OsType.Windows ? ".win" : ".linux";

        if("0".equals(chargingMode))
            return HuaweiServerHelper.getPostPaidServerCost(
                    client,
                    UUID.randomUUID().toString(),
                    serverDetail.get().getFlavor().getId(),
                    osSuffix,
                    serverDetail.get().getOsEXTAZAvailabilityZone()
            );
        else if("1".equals(chargingMode))
            return HuaweiServerHelper.getPrePaidServerCost(
                    client,
                    UUID.randomUUID().toString(),
                    serverDetail.get().getFlavor().getId(),
                    osSuffix,
                    2,
                    1,
                    serverDetail.get().getOsEXTAZAvailabilityZone(),
                    ChronoUnit.MONTHS
            );
        else
            return ResourceCost.ZERO;
    }

    @Override
    public List<ExternalResourceEvent> describeResourceEvents(ExternalAccount account, String externalId, LocalDateTime happenedAfter) {
        HuaweiCloudClient client = provider.buildClient(account);

        List<String> eventNames = HuaweiEventTypes.instanceEventTypes.stream().map(
                HuaweiEventTypes.HuaweiEventType::externalEventName
        ).toList();

        List<Traces> events = client.cts().describeEvents(
                eventNames, "ecs", externalId, happenedAfter
        );

        List<ExternalResourceEvent> result = new ArrayList<>();

        for (Traces event : events) {
            var aliyunEventType = HuaweiEventTypes.fromInstanceEventName(event.getTraceName());

            if(aliyunEventType.isEmpty())
                continue;

            ExternalResourceEvent externalResourceEvent = new ExternalResourceEvent(
                    event.getTraceId(),
                    aliyunEventType.get().eventType(),
                    StratoEventLevel.INFO,
                    StratoEventSource.EXTERNAL_ACTION,
                    getResourceTypeId(),
                    account.getId(),
                    externalId,
                    event.getOperationId(),
                    TimeUtil.fromUtcEpochMillis(event.getRecordTime())
            );
            result.add(
                    externalResourceEvent
            );
        }

        return result;
    }
}
