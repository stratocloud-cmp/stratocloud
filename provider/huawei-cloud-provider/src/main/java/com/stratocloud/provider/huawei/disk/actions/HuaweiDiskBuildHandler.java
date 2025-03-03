package com.stratocloud.provider.huawei.disk.actions;

import com.huaweicloud.sdk.evs.v2.model.BssParamForCreateVolume;
import com.huaweicloud.sdk.evs.v2.model.CreateVolumeOption;
import com.huaweicloud.sdk.evs.v2.model.CreateVolumeRequest;
import com.huaweicloud.sdk.evs.v2.model.CreateVolumeRequestBody;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.disk.HuaweiDiskHandler;
import com.stratocloud.provider.huawei.disk.HuaweiDiskHelper;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceCost;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class HuaweiDiskBuildHandler implements BuildResourceActionHandler {

    private final HuaweiDiskHandler diskHandler;

    public HuaweiDiskBuildHandler(HuaweiDiskHandler diskHandler) {
        this.diskHandler = diskHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return diskHandler;
    }

    @Override
    public String getTaskName() {
        return "创建云硬盘";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return HuaweiDiskBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        if(resource.isPrimaryTo(ResourceCategories.COMPUTE_INSTANCE)) {
            log.warn("Disk {} is expected to be a system disk, skipping BUILD action...", resource.getName());
            return;
        }

        CreateVolumeRequest request = getCreateVolumeRequest(resource, parameters);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) diskHandler.getProvider();

        String volumeId = provider.buildClient(account).evs().createVolume(
                request
        );
        resource.setExternalId(volumeId);
    }

    private static CreateVolumeRequest getCreateVolumeRequest(Resource resource, Map<String, Object> parameters) {
        HuaweiDiskBuildInput input = JSON.convert(parameters, HuaweiDiskBuildInput.class);

        Resource zone = resource.getEssentialTarget(ResourceCategories.ZONE).orElseThrow(
                () -> new StratoException("Zone not found when creating disk.")
        );


        CreateVolumeOption option = new CreateVolumeOption();

        option.setAvailabilityZone(zone.getExternalId());
        option.setSize(input.getSize());
        option.setName(resource.getName());
        option.setDescription(resource.getDescription());
        option.setVolumeType(CreateVolumeOption.VolumeTypeEnum.fromValue(input.getVolumeType()));
        option.setIops(input.getIops());
        option.setThroughput(input.getThroughput());

        BssParamForCreateVolume bssParam = new BssParamForCreateVolume();
        var chargingModeEnum = BssParamForCreateVolume.ChargingModeEnum.fromValue(input.getChargingMode());
        bssParam.setChargingMode(chargingModeEnum);
        bssParam.setIsAutoPay(BssParamForCreateVolume.IsAutoPayEnum.TRUE);

        if(BssParamForCreateVolume.ChargingModeEnum.PREPAID.equals(chargingModeEnum)){
            bssParam.setIsAutoRenew(
                    input.isAutoRenew() ?
                            BssParamForCreateVolume.IsAutoRenewEnum.TRUE :
                            BssParamForCreateVolume.IsAutoRenewEnum.FALSE
            );

            int period = Integer.parseInt(input.getPeriod());

            int periodYears = period / 12;
            int periodMonths = period % 12;

            if(periodYears > 0){
                bssParam.setPeriodNum(periodYears);
                bssParam.setPeriodType(BssParamForCreateVolume.PeriodTypeEnum.YEAR);
            }else {
                bssParam.setPeriodNum(periodMonths);
                bssParam.setPeriodType(BssParamForCreateVolume.PeriodTypeEnum.MONTH);
            }
        }

        return new CreateVolumeRequest().withBody(
                new CreateVolumeRequestBody().withVolume(
                        option
                ).withBssParam(bssParam)
        );
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        HuaweiDiskBuildInput input = JSON.convert(parameters, HuaweiDiskBuildInput.class);

        return List.of(
                new ResourceUsage(
                        UsageTypes.DISK_GB.type(), BigDecimal.valueOf(input.getSize())
                )
        );
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }

    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) diskHandler.getProvider();
        HuaweiCloudClient client = provider.buildClient(account);

        CreateVolumeRequest createVolumeRequest = getCreateVolumeRequest(resource, parameters);
        CreateVolumeOption volume = createVolumeRequest.getBody().getVolume();
        BssParamForCreateVolume bssParam = createVolumeRequest.getBody().getBssParam();

        String inquiryId = UUID.randomUUID().toString();
        String volumeType = volume.getVolumeType().getValue();
        Integer volumeSize = volume.getSize();
        String availabilityZone = volume.getAvailabilityZone();

        if(BssParamForCreateVolume.ChargingModeEnum.PREPAID.equals(bssParam.getChargingMode())) {
            BssParamForCreateVolume.PeriodTypeEnum periodType = bssParam.getPeriodType();
            int periodTypeNumber;
            ChronoUnit timeUnit;
            if(BssParamForCreateVolume.PeriodTypeEnum.YEAR.equals(periodType)) {
                periodTypeNumber = 3;
                timeUnit = ChronoUnit.YEARS;
            } else {
                periodTypeNumber = 2;
                timeUnit = ChronoUnit.MONTHS;
            }

            Integer periodNum = bssParam.getPeriodNum();

            return HuaweiDiskHelper.getPrePaidDiskCost(
                    client, inquiryId,
                    volumeType,
                    periodTypeNumber,
                    periodNum,
                    volumeSize,
                    availabilityZone,
                    timeUnit
            );
        } else {
            return HuaweiDiskHelper.getPostPaidDiskCost(client, inquiryId, volumeType, volumeSize, availabilityZone);
        }
    }


}
