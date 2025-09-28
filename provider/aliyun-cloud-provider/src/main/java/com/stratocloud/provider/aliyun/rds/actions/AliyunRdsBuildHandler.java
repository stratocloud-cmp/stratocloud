package com.stratocloud.provider.aliyun.rds.actions;

import com.aliyun.rds20140815.models.CreateDBInstanceRequest;
import com.aliyun.rds20140815.models.DescribePriceRequest;
import com.aliyun.rds20140815.models.DescribePriceResponseBody;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.rds.AliyunRdsHandler;
import com.stratocloud.provider.aliyun.rds.model.RdsInstanceClass;
import com.stratocloud.provider.aliyun.subnet.AliyunSubnet;
import com.stratocloud.provider.aliyun.vpc.AliyunVpc;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceCost;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class AliyunRdsBuildHandler implements BuildResourceActionHandler {

    private final AliyunRdsHandler rdsHandler;

    public AliyunRdsBuildHandler(AliyunRdsHandler rdsHandler) {
        this.rdsHandler = rdsHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return rdsHandler;
    }

    @Override
    public String getTaskName() {
        return "创建RDS实例";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return AliyunRdsBuildInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null || resource.getAccountId() == null)
            return Optional.empty();

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) rdsHandler.getProvider();

        return Optional.of(
                AliyunRdsBuildInput.getFormMeta(provider.buildClient(account))
        );
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        String instanceId = createRdsInstance(resource, parameters, false);
        resource.setExternalId(instanceId);
    }

    private String createRdsInstance(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        AliyunRdsBuildInput input = JSON.convert(parameters, AliyunRdsBuildInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) rdsHandler.getProvider();
        AliyunClient client = provider.buildClient(account);

        var engineInput = input.getEngineSpecificInput();

        Resource subnetResource = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not provided")
        );

        AliyunSubnet subnet = client.vpc().describeSubnet(subnetResource.getExternalId()).orElseThrow(
                () -> new StratoException("Subnet not found")
        );

        AliyunVpc vpc = client.vpc().describeVpc(subnet.detail().getVpcId()).orElseThrow(
                () -> new StratoException("VPC not found")
        );

        CreateDBInstanceRequest request = new CreateDBInstanceRequest();
        request.setDryRun(dryRun);
        request.setAmount(1);
        request.setAutoCreateProxy(input.isAutoCreateProxy());
        request.setAutoPay(true);
        request.setAutoUseCoupon(input.isAutoUseCoupon());

        request.setPayType(input.getPayType());
        if(Objects.equals(input.getPayType(), "Prepaid")){
            if(input.getPeriod() >= 12){
                request.setPeriod("Year");
                request.setUsedTime(String.valueOf(input.getPeriod() / 12));
            } else {
                request.setPeriod("Month");
                request.setUsedTime(String.valueOf(input.getPeriod()));
            }
            request.setAutoRenew(input.getAutoRenew());
        }

        request.setEngine(input.getEngine().name());
        request.setEngineVersion(engineInput.getEngineVersion());
        request.setCategory(engineInput.getCategory().name());
        request.setDBInstanceClass(engineInput.getClassCode());

        List<AliyunSubnet> subnets = new ArrayList<>();
        subnets.add(subnet);

        Set<RdsInstanceClass.Category> multiZoneCategories = Set.of(
                RdsInstanceClass.Category.HighAvailability,
                RdsInstanceClass.Category.AlwaysOn,
                RdsInstanceClass.Category.cluster
        );
        if(multiZoneCategories.contains(engineInput.getCategory()) && engineInput.isEnableMultiZone()){
            if(engineInput.getSlaveNumber() >= 1 && Utils.isNotBlank(engineInput.getVSwitchIdSlave1())){
                var subnetSlave1 = client.vpc().describeSubnet(engineInput.getVSwitchIdSlave1());
                subnetSlave1.ifPresent(subnets::add);

                if(engineInput.getSlaveNumber() >= 2 && Utils.isNotBlank(engineInput.getVSwitchIdSlave2())){
                    var subnetSlave2 = client.vpc().describeSubnet(engineInput.getVSwitchIdSlave2());
                    subnetSlave2.ifPresent(subnets::add);
                }
            }
        }


        request.setVPCId(vpc.getVpcId());
        request.setVSwitchId(
                String.join(
                        ",",
                        subnets.stream().map(s -> s.detail().getVSwitchId()).toList()
                )
        );

        request.setZoneId(subnet.detail().getZoneId());

        if(subnets.size() > 1){
            request.setZoneIdSlave1(subnets.get(1).detail().getZoneId());

            if(subnets.size() > 2){
                request.setZoneIdSlave2(subnets.get(2).detail().getZoneId());
            }
        }


        request.setDBInstanceDescription(resource.getName());
        request.setDBInstanceNetType("Intranet");
        request.setInstanceNetworkType("VPC");
        request.setPort(String.valueOf(engineInput.getPort()));

        request.setDBInstanceStorage(engineInput.getStorageSize());
        request.setDBInstanceStorageType(engineInput.getStorageType());
        request.setDBIsIgnoreCase(String.valueOf(engineInput.isIgnoreCase()));

        if(engineInput.isGrantVpcAccess()){
            request.setSecurityIPList(vpc.detail().getCidrBlock());
        }

        return client.rds().createInstance(request);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        AliyunRdsBuildInput input = JSON.convert(parameters, AliyunRdsBuildInput.class);

        String classCode = input.getEngineSpecificInput().getClassCode();

        if(Utils.isBlank(classCode))
            return List.of();

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) rdsHandler.getProvider();
        AliyunClient client = provider.buildClient(account);

        Optional<RdsInstanceClass> instanceClass = client.rds().describeInstanceClass(classCode);

        return instanceClass.map(rdsInstanceClass -> List.of(
                new ResourceUsage(
                        UsageTypes.CPU_CORES.type(),
                        BigDecimal.valueOf(rdsInstanceClass.getCpuCores())
                ),
                new ResourceUsage(
                        UsageTypes.MEMORY_GB.type(),
                        BigDecimal.valueOf(rdsInstanceClass.getMemoryGb())
                ),
                new ResourceUsage(
                        UsageTypes.DISK_GB.type(),
                        BigDecimal.valueOf(input.getEngineSpecificInput().getStorageSize())
                )
        )).orElseGet(List::of);

    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createRdsInstance(resource, parameters, true);
    }

    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) rdsHandler.getProvider();
        AliyunClient client = provider.buildClient(account);

        Resource subnetResource = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not provided")
        );

        AliyunSubnet subnet = client.vpc().describeSubnet(subnetResource.getExternalId()).orElseThrow(
                () -> new StratoException("Subnet not found")
        );

        AliyunRdsBuildInput input = JSON.convert(parameters, AliyunRdsBuildInput.class);

        DescribePriceRequest request = new DescribePriceRequest();

        request.setPayType(input.getPayType());
        if(Objects.equals(input.getPayType(), "Prepaid")){
            request.setCommodityCode("rds");

            if(input.getPeriod() >= 12){
                request.setTimeType("Year");
                request.setUsedTime(Math.toIntExact(input.getPeriod() / 12));
            } else {
                request.setTimeType("Month");
                request.setUsedTime(Math.toIntExact(input.getPeriod()));
            }
        } else {
            request.setCommodityCode("bards");
        }

        request.setDBInstanceClass(input.getEngineSpecificInput().getClassCode());
        request.setDBInstanceStorage(input.getEngineSpecificInput().getStorageSize());
        request.setDBInstanceStorageType(input.getEngineSpecificInput().getStorageType());
        request.setZoneId(subnet.detail().getZoneId());
        request.setEngine(input.getEngine().name());
        request.setEngineVersion(input.getEngineSpecificInput().getEngineVersion());
        request.setInstanceUsedType(0);
        request.setOrderType("BUY");
        request.setQuantity(1);

        DescribePriceResponseBody responseBody = client.rds().describePrice(request);

        if(Objects.equals(input.getPayType(), "Prepaid")){
            return new ResourceCost(
                    responseBody.getPriceInfo().getTradePrice(), input.getPeriod(), ChronoUnit.MONTHS
            );
        }else {
            return new ResourceCost(
                    responseBody.getPriceInfo().getTradePrice(), 1.0, ChronoUnit.HOURS
            );
        }
    }
}
