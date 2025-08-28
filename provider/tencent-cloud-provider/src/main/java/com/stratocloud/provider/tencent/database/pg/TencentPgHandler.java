package com.stratocloud.provider.tencent.database.pg;

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
import com.stratocloud.provider.tencent.database.pg.util.PgUtil;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.postgres.v20170312.models.DBInstance;
import com.tencentcloudapi.postgres.v20170312.models.DBInstanceNetInfo;
import com.tencentcloudapi.postgres.v20170312.models.DescribeDBInstancesRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class TencentPgHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    private final IpAllocator ipAllocator;

    public TencentPgHandler(TencentCloudProvider provider,
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
        return "TENCENT_CLOUD_PG";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云PostgreSQL实例";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.RELATIONAL_DB_INSTANCE;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        Optional<DBInstance> pg = describePg(account, externalId);

        return pg.map(i -> toExternalResource(account, i));
    }

    public Optional<DBInstance> describePg(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        TencentCloudClient client = provider.buildClient(account);
        return client.describePgInstance(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, DBInstance pg) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                pg.getDBInstanceId(),
                pg.getDBInstanceName(),
                convertState(pg.getDBInstanceStatus())
        );
    }

    private ResourceState convertState(String status) {
        if(status == null)
            return ResourceState.UNKNOWN;

        return switch (status){
            case "applying", "init", "initing" -> ResourceState.BUILDING;
            case "running", "waitSwitch", "limited run", "readonly" -> ResourceState.STARTED;
            case "recycling", "isolating", "isolated", "disisolating", "recycled" -> ResourceState.SHUTDOWN;
            case "offlining" -> ResourceState.DESTROYING;
            case "offline" -> ResourceState.DESTROYED;
            case "migrating", "job running", "expanding", "switching",
                 "network changing", "upgrading", "audit-switching", "primary-switching",
                 "deployment changing", "cloning", "parameter modifying", "log-switching",
                 "restoring"
                    -> ResourceState.CONFIGURING;
            case "restarting" -> ResourceState.RESTARTING;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);
        return client.describePgInstances(
                new DescribeDBInstancesRequest()
        ).stream().map(
                i -> toExternalResource(account, i)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        DBInstance pg = describePg(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("PG not found: " + resource.getExternalId())
        );

        resource.updateByExternal(toExternalResource(account, pg));

        List<String> privateAddresses = getPrivateAddresses(pg);
        String privateAddressesStr = String.join(",", privateAddresses);
        RuntimeProperty privateAddressesProperty = RuntimeProperty.ofDisplayInList(
                "privateAddresses",
                "内网地址",
                privateAddressesStr,
                privateAddressesStr
        );
        resource.addOrUpdateRuntimeProperty(privateAddressesProperty);

        List<String> publicAddresses = getPublicAddresses(pg);

        if(!publicAddresses.isEmpty()){
            String publicAddressesStr = String.join(",", publicAddresses);
            RuntimeProperty publicAddressesProperty = RuntimeProperty.ofDisplayInList(
                    "publicAddresses",
                    "外网地址",
                    publicAddressesStr,
                    publicAddressesStr
            );
            resource.addOrUpdateRuntimeProperty(publicAddressesProperty);
        }


        String sellConfigSummary = "%s核%sGB/%sGB".formatted(
                pg.getDBInstanceCpu(),
                pg.getDBInstanceMemory(),
                pg.getDBInstanceStorage()
        );
        RuntimeProperty sellConfigProperty = RuntimeProperty.ofDisplayInList(
                "sellConfig",
                "配置",
                sellConfigSummary,
                sellConfigSummary
        );
        resource.addOrUpdateRuntimeProperty(sellConfigProperty);


        updateInstanceRoleProperties(account, resource, pg);


        resource.updateUsageByType(UsageTypes.CPU_CORES, BigDecimal.valueOf(pg.getDBInstanceCpu()));
        resource.updateUsageByType(UsageTypes.MEMORY_GB, BigDecimal.valueOf(pg.getDBInstanceMemory()));
        resource.updateUsageByType(UsageTypes.DISK_GB, BigDecimal.valueOf(pg.getDBInstanceStorage()));

        resource.getEssentialTarget(
                ResourceCategories.SUBNET
        ).ifPresent(
                s -> ipAllocator.forceAllocateIps(
                        s, InternetProtocol.IPv4, getPrivateIps(pg), resource
                )
        );
    }

    private List<String> getPrivateAddresses(DBInstance pg) {
        if(Utils.isEmpty(pg.getDBInstanceNetInfo()))
            return List.of();

        return Arrays.stream(pg.getDBInstanceNetInfo()).filter(
                n -> Set.of("inner", "private").contains(n.getNetType())
        ).map(
                n -> n.getIp() + ":" + n.getPort()
        ).toList();
    }

    private List<String> getPublicAddresses(DBInstance pg) {
        if(Utils.isEmpty(pg.getDBInstanceNetInfo()))
            return List.of();

        return Arrays.stream(pg.getDBInstanceNetInfo()).filter(
                n -> Objects.equals("public", n.getNetType())
        ).map(
                n -> n.getIp() + ":" + n.getPort()
        ).toList();
    }

    private List<String> getPrivateIps(DBInstance pg){
        if(Utils.isEmpty(pg.getDBInstanceNetInfo()))
            return List.of();

        return Arrays.stream(pg.getDBInstanceNetInfo()).filter(
                n -> Set.of("inner", "private").contains(n.getNetType())
        ).map(DBInstanceNetInfo::getIp).toList();
    }




    private void updateInstanceRoleProperties(ExternalAccount account, Resource resource, DBInstance instance) {
        RuntimeProperty instanceRoleProperty = RuntimeProperty.ofDisplayInList(
                "instanceRole",
                "角色",
                instance.getDBInstanceType(),
                instance.getDBInstanceType()
        );
        resource.addOrUpdateRuntimeProperty(instanceRoleProperty);

        if(Utils.isNotBlank(instance.getMasterDBInstanceId())){
            Optional<DBInstance> master
                    = provider.buildClient(account).describePgInstance(instance.getMasterDBInstanceId());

            if(master.isPresent()){
                RuntimeProperty masterProperty = RuntimeProperty.ofDisplayable(
                        "master",
                        "主实例",
                        master.get().getDBInstanceId(),
                        master.get().getDBInstanceName()
                );
                resource.addOrUpdateRuntimeProperty(masterProperty);
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
        return PgUtil.getPgInstanceCost(resource, 1L);
    }

}
