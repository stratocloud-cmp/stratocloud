package com.stratocloud.provider.huawei.common;

import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.ExternalAccountProperties;
import lombok.Data;

@Data
public class HuaweiCloudAccountProperties implements ExternalAccountProperties {
    @InputField(label = "AK", description = "Access Key ID", inputType = "textarea")
    private String accessKeyId;
    @InputField(label = "SK", description = "Secret Access Key", inputType = "textarea")
    private String secretAccessKey;
    @SelectField(
            label = "地域",
            allowCreate = true,
            description = "若所需地域在选项中不存在，可直接输入其地域ID。",
            options = {
                    HuaweiCloudRegion.Ids.CN_EAST_2,
                    HuaweiCloudRegion.Ids.CN_EAST_3,
                    HuaweiCloudRegion.Ids.CN_EAST_4,
                    HuaweiCloudRegion.Ids.CN_EAST_5,
                    HuaweiCloudRegion.Ids.CN_NORTH_1,
                    HuaweiCloudRegion.Ids.CN_NORTH_2,
                    HuaweiCloudRegion.Ids.CN_NORTH_4,
                    HuaweiCloudRegion.Ids.CN_NORTH_9,
                    HuaweiCloudRegion.Ids.CN_SOUTH_1,
                    HuaweiCloudRegion.Ids.CN_SOUTH_2,
                    HuaweiCloudRegion.Ids.CN_SOUTH_4,
                    HuaweiCloudRegion.Ids.CN_SOUTHWEST_2,
                    HuaweiCloudRegion.Ids.AE_AD_1,
                    HuaweiCloudRegion.Ids.AF_NORTH_1,
                    HuaweiCloudRegion.Ids.AF_SOUTH_1,
                    HuaweiCloudRegion.Ids.AP_SOUTHEAST_1,
                    HuaweiCloudRegion.Ids.AP_SOUTHEAST_2,
                    HuaweiCloudRegion.Ids.AP_SOUTHEAST_3,
                    HuaweiCloudRegion.Ids.AP_SOUTHEAST_4,
                    HuaweiCloudRegion.Ids.AP_SOUTHEAST_5,
                    HuaweiCloudRegion.Ids.EU_WEST_0,
                    HuaweiCloudRegion.Ids.LA_NORTH_2,
                    HuaweiCloudRegion.Ids.LA_SOUTH_2,
                    HuaweiCloudRegion.Ids.ME_EAST_1,
                    HuaweiCloudRegion.Ids.MY_KUALA_LUMPUR_1,
                    HuaweiCloudRegion.Ids.NA_MEXICO_1,
                    HuaweiCloudRegion.Ids.RU_MOSCOW_1,
                    HuaweiCloudRegion.Ids.SA_BRAZIL_1,
                    HuaweiCloudRegion.Ids.TR_WEST_1
            },
            optionNames = {
                    HuaweiCloudRegion.Names.CN_EAST_2,
                    HuaweiCloudRegion.Names.CN_EAST_3,
                    HuaweiCloudRegion.Names.CN_EAST_4,
                    HuaweiCloudRegion.Names.CN_EAST_5,
                    HuaweiCloudRegion.Names.CN_NORTH_1,
                    HuaweiCloudRegion.Names.CN_NORTH_2,
                    HuaweiCloudRegion.Names.CN_NORTH_4,
                    HuaweiCloudRegion.Names.CN_NORTH_9,
                    HuaweiCloudRegion.Names.CN_SOUTH_1,
                    HuaweiCloudRegion.Names.CN_SOUTH_2,
                    HuaweiCloudRegion.Names.CN_SOUTH_4,
                    HuaweiCloudRegion.Names.CN_SOUTHWEST_2,
                    HuaweiCloudRegion.Names.AE_AD_1,
                    HuaweiCloudRegion.Names.AF_NORTH_1,
                    HuaweiCloudRegion.Names.AF_SOUTH_1,
                    HuaweiCloudRegion.Names.AP_SOUTHEAST_1,
                    HuaweiCloudRegion.Names.AP_SOUTHEAST_2,
                    HuaweiCloudRegion.Names.AP_SOUTHEAST_3,
                    HuaweiCloudRegion.Names.AP_SOUTHEAST_4,
                    HuaweiCloudRegion.Names.AP_SOUTHEAST_5,
                    HuaweiCloudRegion.Names.EU_WEST_0,
                    HuaweiCloudRegion.Names.LA_NORTH_2,
                    HuaweiCloudRegion.Names.LA_SOUTH_2,
                    HuaweiCloudRegion.Names.ME_EAST_1,
                    HuaweiCloudRegion.Names.MY_KUALA_LUMPUR_1,
                    HuaweiCloudRegion.Names.NA_MEXICO_1,
                    HuaweiCloudRegion.Names.RU_MOSCOW_1,
                    HuaweiCloudRegion.Names.SA_BRAZIL_1,
                    HuaweiCloudRegion.Names.TR_WEST_1
            }
    )
    private String region;
}
