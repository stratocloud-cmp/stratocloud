package com.stratocloud.provider.tencent.database.cdb;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.ip.IpAllocator;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.common.TencentCloudRegion;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cdb.v20170320.models.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class TencentCdbHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    private final IpAllocator ipAllocator;

    public TencentCdbHandler(TencentCloudProvider provider,
                             IpAllocator ipAllocator) {
        this.provider = provider;
        this.ipAllocator = ipAllocator;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_CLOUD_CDB";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云MySQL云数据库";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.CLOUD_RELATIONAL_DATABASE;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        Optional<InstanceInfo> cdb = describeCdb(account, externalId);

        return cdb.map(i -> toExternalResource(account, i));
    }

    public Optional<InstanceInfo> describeCdb(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        TencentCloudClient client = provider.buildClient(account);
        return client.describeCdbInstance(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, InstanceInfo cdb) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                cdb.getInstanceId(),
                cdb.getInstanceName(),
                convertState(cdb.getStatus())
        );
    }

    private ResourceState convertState(Long status) {
        if(status == null)
            return ResourceState.UNKNOWN;

        return switch (status.intValue()){
            case 0 -> ResourceState.BUILDING;
            case 1 -> ResourceState.STARTED;
            case 4, 5 -> ResourceState.SHUTDOWN;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);
        return client.describeCdbInstances(
                new DescribeDBInstancesRequest()
        ).stream().map(
                i -> toExternalResource(account, i)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        InstanceInfo cdb = describeCdb(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("CDB not found: " + resource.getExternalId())
        );

        resource.updateByExternal(toExternalResource(account, cdb));

        RuntimeProperty vipProperty = RuntimeProperty.ofDisplayInList(
                "vip",
                "IP",
                cdb.getVip(),
                cdb.getVip()
        );
        resource.addOrUpdateRuntimeProperty(vipProperty);

        String sellConfigSummary = "%s-%s核%sMB/%sGB".formatted(
                CdbDeviceType.fromString(cdb.getDeviceType()).getLabel(),
                cdb.getCpu(),
                cdb.getMemory(),
                cdb.getVolume()
        );
        RuntimeProperty sellConfigProperty = RuntimeProperty.ofDisplayInList(
                "sellConfig",
                "配置",
                sellConfigSummary,
                sellConfigSummary
        );
        resource.addOrUpdateRuntimeProperty(sellConfigProperty);


        RuntimeProperty instanceNodesProperty = RuntimeProperty.ofDisplayInList(
                "instanceNodes",
                "节点数",
                String.valueOf(cdb.getInstanceNodes()),
                String.valueOf(cdb.getInstanceNodes())
        );
        resource.addOrUpdateRuntimeProperty(instanceNodesProperty);

        updateInstanceRoleProperties(account, resource, cdb);


        resource.updateUsageByType(UsageTypes.CPU_CORES, BigDecimal.valueOf(cdb.getCpu()));
        resource.updateUsageByType(UsageTypes.MEMORY_GB, BigDecimal.valueOf(cdb.getMemory()/1024.0));
        resource.updateUsageByType(UsageTypes.DISK_GB, BigDecimal.valueOf(cdb.getVolume()));

        resource.getEssentialTarget(
                ResourceCategories.SUBNET
        ).ifPresent(
                s -> ipAllocator.forceAllocateIps(s, InternetProtocol.IPv4, List.of(cdb.getVip()), resource)
        );
    }

    private void updateInstanceRoleProperties(ExternalAccount account, Resource resource, InstanceInfo cdb) {
        CdbInstanceRole instanceRole = CdbInstanceRole.fromLong(cdb.getInstanceType());
        RuntimeProperty instanceRoleProperty = RuntimeProperty.ofDisplayInList(
                "instanceRole",
                "角色",
                instanceRole.name(),
                instanceRole.name()
        );
        resource.addOrUpdateRuntimeProperty(instanceRoleProperty);

        if(instanceRole == CdbInstanceRole.dr || instanceRole == CdbInstanceRole.ro){
            MasterInfo masterInfo = cdb.getMasterInfo();

            if(masterInfo != null){
                Optional<TencentCloudRegion> region = TencentCloudRegion.fromId(masterInfo.getRegion());

                if(region.isPresent()){
                    TencentCloudClient clientWithRegion = provider.buildClientWithRegion(account, region.get());

                    Optional<InstanceInfo> master = clientWithRegion.describeCdbInstance(masterInfo.getInstanceId());

                    if(master.isPresent()){
                        RuntimeProperty masterProperty = RuntimeProperty.ofDisplayable(
                                "master",
                                "主实例",
                                master.get().getInstanceId(),
                                master.get().getInstanceName()
                        );
                        resource.addOrUpdateRuntimeProperty(masterProperty);
                    }


                    if (master.isPresent() && instanceRole == CdbInstanceRole.ro) {
                        if(Utils.isNotEmpty(master.get().getRoGroups())){
                            for (RoGroup roGroup : master.get().getRoGroups()) {
                                boolean inGroup = Arrays.stream(roGroup.getRoInstances()).anyMatch(
                                        i -> Objects.equals(cdb.getInstanceId(), i.getInstanceId())
                                );

                                if(inGroup){
                                    RuntimeProperty roGroupProperty = RuntimeProperty.ofDisplayable(
                                            "roGroup",
                                            "RO组",
                                            roGroup.getRoGroupId(),
                                            "%s (ID: %s)".formatted(roGroup.getRoGroupName(), roGroup.getRoGroupId())
                                    );
                                    resource.addOrUpdateRuntimeProperty(roGroupProperty);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of(
                UsageTypes.CPU_CORES, UsageTypes.MEMORY_GB, UsageTypes.DISK_GB
        );
    }

    @Override
    public ResourceCost getCurrentCost(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        Optional<InstanceInfo> instanceInfo = describeCdb(account, resource.getExternalId());

        if(instanceInfo.isEmpty())
            return ResourceCost.ZERO;

        InstanceInfo cdb = instanceInfo.get();

        DescribeDBPriceRequest request = new DescribeDBPriceRequest();

        double timeAmount;
        ChronoUnit timeUnit;

        if(CdbUtil.isPrepaid(cdb)){
            request.setPayType("PRE_PAID");
            request.setPeriod(1L);

            timeAmount = 1;
            timeUnit = ChronoUnit.MONTHS;
        } else {
            request.setPayType("HOUR_PAID");
            request.setPeriod(1L);
            request.setLadder(3L);

            timeAmount = 1;
            timeUnit = ChronoUnit.HOURS;
        }

        request.setGoodsNum(1L);

        request.setZone(cdb.getZone());
        request.setInstanceRole(CdbInstanceRole.fromLong(cdb.getInstanceType()).name());
        request.setInstanceNodes(cdb.getInstanceNodes());

        request.setDeviceType(cdb.getDeviceType());
        request.setDiskType(cdb.getDiskType());
        request.setMemory(cdb.getMemory());
        request.setVolume(cdb.getVolume());
        request.setCpu(cdb.getCpu());

        DescribeDBPriceResponse response = client.describeCdbPrice(request);

        Long price = response.getPrice();

        if(price == null)
            return ResourceCost.ZERO;

        return new ResourceCost(price/100.0, timeAmount, timeUnit);
    }
}
