package com.stratocloud.provider.aliyun.rds;

import com.aliyun.rds20140815.models.DescribeDBInstancesRequest;
import com.aliyun.rds20140815.models.DescribePriceRequest;
import com.aliyun.rds20140815.models.DescribePriceResponseBody;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.ip.IpAllocator;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.common.services.AliyunRdsService;
import com.stratocloud.provider.aliyun.rds.model.RdsInstance;
import com.stratocloud.provider.aliyun.rds.model.RdsInstanceClass;
import com.stratocloud.provider.aliyun.rds.model.RdsInstanceDetail;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class AliyunRdsHandler extends AbstractResourceHandler {

    private final AliyunCloudProvider provider;

    private final IpAllocator ipAllocator;

    public AliyunRdsHandler(AliyunCloudProvider provider,
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
        return "ALIYUN_CLOUD_CDB";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云RDS实例";
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
        Optional<RdsInstance> rds = describeRds(account, externalId);

        return rds.map(i -> toExternalResource(account, i));
    }

    public Optional<RdsInstance> describeRds(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        AliyunClient client = provider.buildClient(account);
        return client.rds().describeInstance(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, RdsInstance rds) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                rds.detail().getDBInstanceId(),
                rds.detail().getDBInstanceDescription(),
                convertState(rds.detail().getDBInstanceStatus())
        );
    }


    private ResourceState convertState(String status) {
        if(status == null)
            return ResourceState.UNKNOWN;

        return switch (status){
            case "Creating" -> ResourceState.BUILDING;
            case "Running" -> ResourceState.STARTED;
            case "Deleting", "Released" -> ResourceState.SHUTDOWN;
            case "Rebooting" -> ResourceState.RESTARTING;
            case "Stopping" -> ResourceState.STOPPING;
            case "Stopped" -> ResourceState.STOPPED;
            case "DBInstanceClassChanging", "TRANSING", "EngineVersionUpgrading", "TransingToOthers",
                 "GuardDBInstanceCreating", "Restoring", "Importing", "ImportingFromOthers",
                 "DBInstanceNetTypeChanging", "GuardSwitching", "INS_CLONING"
                    -> ResourceState.CONFIGURING;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        AliyunClient client = provider.buildClient(account);
        return client.rds().describeInstances(
                new DescribeDBInstancesRequest()
        ).stream().map(
                i -> toExternalResource(account, i)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            throw new ExternalResourceNotFoundException("RDS not found: " + resource.getExternalId());

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        AliyunRdsService rdsService = provider.buildClient(account).rds();
        RdsInstanceDetail instanceDetail = rdsService.describeInstanceDetail(resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("RDS not found: " + resource.getExternalId())
        );

        resource.updateByExternal(toExternalResource(account, instanceDetail.instance()));

        var netInfo = instanceDetail.netInfo();
        if(Utils.isNotEmpty(netInfo)){
            for (int i = 0; i < netInfo.size(); i++) {
                var net = netInfo.get(i);

                String suffix = netInfo.size() == 1 ? "" : String.valueOf(i+1);

                RuntimeProperty ipProperty = RuntimeProperty.ofDisplayInList(
                        "ip"+suffix,
                        "IP地址"+suffix,
                        net.getIPAddress(),
                        net.getIPAddress()
                );
                resource.addOrUpdateRuntimeProperty(ipProperty);

                RuntimeProperty portProperty = RuntimeProperty.ofDisplayInList(
                        "port"+suffix,
                        "端口"+suffix,
                        net.getPort(),
                        net.getPort()
                );
                resource.addOrUpdateRuntimeProperty(portProperty);

                RuntimeProperty.ofDisplayable(
                        "url"+suffix,
                        "连接地址"+suffix,
                        net.getConnectionString(),
                        net.getConnectionString()
                );

                resource.getEssentialTarget(
                        ResourceCategories.SUBNET
                ).ifPresent(
                        s -> ipAllocator.forceAllocateIps(s, InternetProtocol.IPv4, List.of(net.getIPAddress()), resource)
                );
            }
        }

        Optional<RdsInstanceClass> instanceClass = rdsService.describeInstanceClass(
                instanceDetail.instance().detail().getDBInstanceClass()
        );

        if(instanceClass.isPresent()){
            RuntimeProperty classProperty = RuntimeProperty.ofDisplayInList(
                    "instanceClass",
                    "配置",
                    instanceClass.get().getName(),
                    instanceClass.get().getName()
            );
            resource.addOrUpdateRuntimeProperty(classProperty);


        }


        resource.updateUsageByType(
                UsageTypes.CPU_CORES,
                new BigDecimal(
                        instanceDetail.instance().detail().getDBInstanceCPU()
                )
        );
        resource.updateUsageByType(
                UsageTypes.MEMORY_GB,
                new BigDecimal(
                        instanceDetail.instance().detail().getDBInstanceMemory() / 1024
                )
        );
        resource.updateUsageByType(
                UsageTypes.DISK_GB,
                new BigDecimal(
                        instanceDetail.attributes().getDBInstanceStorage()
                )
        );
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
        AliyunClient client = provider.buildClient(account);

        if(Utils.isBlank(resource.getExternalId()))
            return ResourceCost.ZERO;

        Optional<RdsInstanceDetail> optional = client.rds().describeInstanceDetail(resource.getExternalId());

        if(optional.isEmpty())
            return ResourceCost.ZERO;

        RdsInstanceDetail instance = optional.get();

        DescribePriceRequest request = getDescribePriceRequest(instance);

        DescribePriceResponseBody responseBody = client.rds().describePrice(request);

        if(Objects.equals(instance.instance().detail().getPayType(), "Prepaid")){
            return new ResourceCost(
                    responseBody.getPriceInfo().getTradePrice(),
                    instance.instance().getMonthPeriod(),
                    ChronoUnit.MONTHS
            );
        }else {
            return new ResourceCost(
                    responseBody.getPriceInfo().getTradePrice(),
                    1.0,
                    ChronoUnit.HOURS
            );
        }
    }

    private static DescribePriceRequest getDescribePriceRequest(RdsInstanceDetail instance) {
        DescribePriceRequest request = new DescribePriceRequest();

        request.setPayType(instance.instance().detail().getPayType());
        if(Objects.equals(instance.instance().detail().getPayType(), "Prepaid")){
            request.setCommodityCode("rds");

            int monthPeriod = instance.instance().getMonthPeriod();

            if(monthPeriod >= 12){
                request.setTimeType("Year");
                request.setUsedTime(monthPeriod / 12);
            } else {
                request.setTimeType("Month");
                request.setUsedTime(monthPeriod);
            }
        } else {
            request.setCommodityCode("bards");
        }

        request.setDBInstanceClass(instance.instance().detail().getDBInstanceClass());
        request.setDBInstanceStorage(instance.attributes().getDBInstanceStorage());
        request.setDBInstanceStorageType(instance.instance().detail().getDBInstanceStorageType());
        request.setZoneId(instance.instance().detail().getZoneId());
        request.setEngine(instance.instance().detail().getEngine());
        request.setEngineVersion(instance.instance().detail().getEngineVersion());
        request.setInstanceUsedType(0);
        request.setOrderType("BUY");
        request.setQuantity(1);
        return request;
    }
}
