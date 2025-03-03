package com.stratocloud.provider.huawei.servers.actions;

import com.huaweicloud.sdk.ecs.v2.model.*;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.ip.IpAllocator;
import com.stratocloud.job.TaskState;
import com.stratocloud.provider.RuntimePropertiesUtil;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.disk.actions.HuaweiDiskBuildInput;
import com.stratocloud.provider.huawei.nic.actions.HuaweiNicBuildInput;
import com.stratocloud.provider.huawei.servers.HuaweiServerHandler;
import com.stratocloud.provider.huawei.servers.HuaweiServerHelper;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.RandomUtil;
import com.stratocloud.utils.SecurityUtil;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class HuaweiServerBuildHandler implements BuildResourceActionHandler {

    private final HuaweiServerHandler serverHandler;

    private final IpAllocator ipAllocator;

    public HuaweiServerBuildHandler(HuaweiServerHandler serverHandler, IpAllocator ipAllocator) {
        this.serverHandler = serverHandler;
        this.ipAllocator = ipAllocator;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return serverHandler;
    }

    @Override
    public String getTaskName() {
        return "创建云主机";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return HuaweiServerBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ServerSpec serverSpec = getServerSpec(resource, parameters);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) serverHandler.getProvider();

        String serverId;
        if(serverSpec.usingPrePaid())
            serverId = provider.buildClient(account).ecs().createPrePaidServer(
                    new CreateServersRequest().withBody(
                            new CreateServersRequestBody().withServer(serverSpec.prePaid())
                    )
            );
        else
            serverId = provider.buildClient(account).ecs().createPostPaidServer(
                    new CreatePostPaidServersRequest().withBody(
                            new CreatePostPaidServersRequestBody().withServer(serverSpec.postPaid())
                    )
            );
        resource.setExternalId(serverId);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ResourceActionResult result = BuildResourceActionHandler.super.checkActionResult(resource, parameters);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) serverHandler.getProvider();
        HuaweiCloudClient client = provider.buildClient(account);

        if(result.taskState() == TaskState.FINISHED){
            setPrimaryNicId(client, resource);
            setSystemDiskId(client, resource);
        }

        return result;
    }

    private void setSystemDiskId(HuaweiCloudClient client, Resource server) {
        Optional<ServerBlockDevice> blockDevice = client.ecs().describeServerSystemDisk(server.getExternalId());
        Optional<Resource> diskResource = server.getPrimaryCapability(ResourceCategories.DISK);

        diskResource.ifPresent(d -> d.setExternalId(blockDevice.orElseThrow().getVolumeId()));
    }

    private void setPrimaryNicId(HuaweiCloudClient client, Resource server) {
        InterfaceAttachment attachment = client.ecs().listServerInterfaces(
                server.getExternalId()
        ).stream().findFirst().orElseThrow();

        Optional<Resource> nicResource = server.getPrimaryCapability(ResourceCategories.NIC);
        nicResource.ifPresent(n -> n.setExternalId(attachment.getPortId()));
    }

    private ServerSpec getServerSpec(Resource resource, Map<String, Object> parameters) {
        var input = JSON.convert(parameters, HuaweiServerBuildInput.class);
        boolean usingPrePaid = input.isPrepaid();

        ServerSpec serverSpec = new ServerSpec(new PrePaidServer(), new PostPaidServer(), usingPrePaid);

        resolvePlacement(resource, serverSpec);

        resolveBasicOptions(resource, serverSpec);

        resolveChargeOptions(serverSpec, input);

        resolveGuestOptions(resource, serverSpec, input);

        resolveSystemDisk(resource, serverSpec);
        resolvePrimaryNic(resource, serverSpec);

        return serverSpec;
    }

    private void resolveChargeOptions(ServerSpec serverSpec,
                                      HuaweiServerBuildInput input) {
        if(serverSpec.usingPrePaid()){
            int period = Integer.parseInt(input.getPeriod());
            int periodYears = period / 12;
            int periodMonths = period % 12;

            PrePaidServerExtendParam extendParam = new PrePaidServerExtendParam();

            if(periodYears > 0)
                extendParam.withPeriodNum(periodYears).withPeriodType(PrePaidServerExtendParam.PeriodTypeEnum.YEAR);
            else
                extendParam.withPeriodNum(periodMonths).withPeriodType(PrePaidServerExtendParam.PeriodTypeEnum.MONTH);

            extendParam.withIsAutoPay(
                    PrePaidServerExtendParam.IsAutoPayEnum.TRUE
            ).withIsAutoRenew(
                    input.isAutoRenew() ?
                            PrePaidServerExtendParam.IsAutoRenewEnum.TRUE :
                            PrePaidServerExtendParam.IsAutoRenewEnum.FALSE
            );
        }
    }

    private record ServerSpec(PrePaidServer prePaid, PostPaidServer postPaid, boolean usingPrePaid){}

    private void resolvePrimaryNic(Resource resource, ServerSpec serverSpec) {
        Resource primaryNic = selectPrimaryNic(resource).orElseThrow(
                () -> new StratoException("No nics found when creating server.")
        );

        Resource subnet = primaryNic.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not provided when creating server")
        );

        Resource vpc = subnet.getEssentialTarget(ResourceCategories.VPC).orElseThrow(
                () -> new StratoException("Vpc not provided when creating server")
        );

        var nicBuildInput = JSON.convert(primaryNic.getProperties(), HuaweiNicBuildInput.class);

        PrePaidServerNic prePaidServerNic = new PrePaidServerNic().withSubnetId(subnet.getExternalId());
        PostPaidServerNic postPaidServerNic = new PostPaidServerNic().withSubnetId(subnet.getExternalId());
        if(Utils.isNotEmpty(nicBuildInput.getIps())) {
            String ipAddress = nicBuildInput.getIps().get(0);
            ipAllocator.allocateIps(subnet, InternetProtocol.IPv4, List.of(ipAddress), primaryNic);
            prePaidServerNic.withIpAddress(ipAddress);
            postPaidServerNic.withIpAddress(ipAddress);
        }
        serverSpec.prePaid().withVpcid(vpc.getExternalId()).addNicsItem(
                prePaidServerNic
        );
        serverSpec.postPaid().withVpcid(vpc.getExternalId()).addNicsItem(
                postPaidServerNic
        );
    }

    private static Optional<Resource> selectPrimaryNic(Resource server) {
        return server.getPrimaryCapability(ResourceCategories.NIC);
    }

    private void resolveSystemDisk(Resource resource, ServerSpec serverSpec) {
        Resource systemDisk = resource.getPrimaryCapability(ResourceCategories.DISK).orElseThrow(
                () -> new StratoException("System disk not found when creating server.")
        );

        var diskInput = JSON.convert(systemDisk.getProperties(), HuaweiDiskBuildInput.class);

        serverSpec.prePaid().withRootVolume(
                new PrePaidServerRootVolume()
                        .withSize(diskInput.getSize())
                        .withVolumetype(
                                PrePaidServerRootVolume.VolumetypeEnum.fromValue(diskInput.getVolumeType())
                        )
                        .withIops(diskInput.getIops())
                        .withThroughput(diskInput.getThroughput())
        );

        serverSpec.postPaid().withRootVolume(
                new PostPaidServerRootVolume()
                        .withSize(diskInput.getSize())
                        .withVolumetype(
                                PostPaidServerRootVolume.VolumetypeEnum.fromValue(diskInput.getVolumeType())
                        )
                        .withIops(diskInput.getIops())
                        .withThroughput(diskInput.getThroughput())
        );
    }

    private static void resolveGuestOptions(Resource resource,
                                            ServerSpec serverSpec,
                                            HuaweiServerBuildInput input) {
        Optional<Resource> keyPair = resource.getExclusiveTarget(ResourceCategories.KEY_PAIR);
        if(keyPair.isPresent()){
            serverSpec.prePaid().withKeyName(keyPair.get().getExternalId());
            serverSpec.postPaid().withKeyName(keyPair.get().getExternalId());
        }

        if(Utils.isNotBlank(input.getUserData())) {
            serverSpec.prePaid().withUserData(SecurityUtil.encodeToBase64(input.getUserData()));
            serverSpec.postPaid().withUserData(SecurityUtil.encodeToBase64(input.getUserData()));
        }

        String adminPass;
        if(Utils.isNotBlank(input.getAdminPass()))
            adminPass = input.getAdminPass();
        else
            adminPass = RandomUtil.generatePasswordLen12();

        serverSpec.prePaid().withAdminPass(adminPass);
        serverSpec.postPaid().withAdminPass(adminPass);

        RuntimePropertiesUtil.setManagementPassword(resource, adminPass);
    }

    private static void resolveBasicOptions(Resource resource, ServerSpec serverSpec) {
        Resource flavor = resource.getEssentialTarget(ResourceCategories.FLAVOR).orElseThrow(
                () -> new StratoException("Flavor not found when creating server.")
        );
        Resource image = resource.getEssentialTarget(ResourceCategories.IMAGE).orElseThrow(
                () -> new StratoException("Image not found when creating server.")
        );

        serverSpec.prePaid()
                .withFlavorRef(flavor.getExternalId())
                .withImageRef(image.getExternalId())
                .withName(resource.getName())
                .withDescription(resource.getDescription());
        serverSpec.postPaid()
                .withFlavorRef(flavor.getExternalId())
                .withImageRef(image.getExternalId())
                .withName(resource.getName())
                .withDescription(resource.getDescription());
    }

    private static void resolvePlacement(Resource resource, ServerSpec serverSpec) {
        Resource zone = resource.getEssentialTarget(ResourceCategories.ZONE).orElseThrow(
                () -> new StratoException("Zone not found when creating server.")
        );

        serverSpec.prePaid().withAvailabilityZone(zone.getExternalId());
        serverSpec.postPaid().withAvailabilityZone(zone.getExternalId());
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        Resource flavorResource = resource.getEssentialTarget(ResourceCategories.FLAVOR).orElseThrow(
                () -> new StratoException("Flavor not found when creating server.")
        );

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) serverHandler.getProvider();
        Optional<Flavor> flavor
                = provider.buildClient(account).ecs().describeFlavor(flavorResource.getExternalId());

        return flavor.map(flv -> List.of(
                new ResourceUsage(UsageTypes.CPU_CORES.type(), new BigDecimal(flv.getVcpus())),
                new ResourceUsage(UsageTypes.MEMORY_GB.type(), BigDecimal.valueOf(flv.getRam() >> 10))
        )).orElseGet(List::of);

    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        ServerSpec serverSpec = getServerSpec(resource, parameters);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) serverHandler.getProvider();

        if(serverSpec.usingPrePaid())
            provider.buildClient(account).ecs().createPrePaidServer(
                    new CreateServersRequest().withBody(
                            new CreateServersRequestBody().withDryRun(true).withServer(serverSpec.prePaid())
                    )
            );
        else
            provider.buildClient(account).ecs().createPostPaidServer(
                    new CreatePostPaidServersRequest().withBody(
                            new CreatePostPaidServersRequestBody().withDryRun(true).withServer(serverSpec.postPaid())
                    )
            );
    }

    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        ServerSpec serverSpec = getServerSpec(resource, parameters);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) serverHandler.getProvider();
        HuaweiCloudClient client = provider.buildClient(account);

        String osSuffix = serverHandler.getOsType(resource) == OsType.Windows ? ".win" : ".linux";
        String inquiryId = UUID.randomUUID().toString();

        if(serverSpec.usingPrePaid()) {
            PrePaidServerExtendParam.PeriodTypeEnum periodType = serverSpec.prePaid().getExtendparam().getPeriodType();
            int periodTypeNumber;
            ChronoUnit timeUnit;
            if(PrePaidServerExtendParam.PeriodTypeEnum.YEAR.equals(periodType)) {
                periodTypeNumber = 3;
                timeUnit = ChronoUnit.YEARS;
            } else {
                periodTypeNumber = 2;
                timeUnit = ChronoUnit.MONTHS;
            }

            String flavorId = serverSpec.prePaid().getFlavorRef();
            Integer periodNum = serverSpec.prePaid().getExtendparam().getPeriodNum();
            String availabilityZone = serverSpec.prePaid().getAvailabilityZone();

            return HuaweiServerHelper.getPrePaidServerCost(
                    client, inquiryId,
                    flavorId,
                    osSuffix,
                    periodTypeNumber,
                    periodNum,
                    availabilityZone,
                    timeUnit
            );
        } else {
            String flavorId = serverSpec.prePaid().getFlavorRef();
            String availabilityZone = serverSpec.prePaid().getAvailabilityZone();

            return HuaweiServerHelper.getPostPaidServerCost(client, inquiryId, flavorId, osSuffix, availabilityZone);
        }
    }


}
