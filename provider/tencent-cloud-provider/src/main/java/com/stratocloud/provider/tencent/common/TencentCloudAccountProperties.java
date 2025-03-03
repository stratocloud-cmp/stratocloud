package com.stratocloud.provider.tencent.common;

import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.ExternalAccountProperties;
import lombok.Data;

@Data
public class TencentCloudAccountProperties implements ExternalAccountProperties {
    @InputField(label = "SecretId", description = "API密钥的SecretId", inputType = "textarea")
    private String secretId;
    @InputField(label = "SecretKey", description = "API密钥的SecretKey", inputType = "textarea")
    private String secretKey;
    @SelectField(
            label = "地域",
            allowCreate = true,
            description = "若所需地域在选项中不存在，可直接输入其地域ID。",
            options = {
                    TencentCloudRegion.Ids.AP_BEIJING,
                    TencentCloudRegion.Ids.AP_GUANGZHOU,
                    TencentCloudRegion.Ids.AP_HONGKONG,
                    TencentCloudRegion.Ids.AP_SHANGHAI,
                    TencentCloudRegion.Ids.AP_SHANGHAI_FSI,
                    TencentCloudRegion.Ids.AP_SHENZHEN_FSI,
                    TencentCloudRegion.Ids.AP_SINGAPORE,
                    TencentCloudRegion.Ids.NA_SILICONVALLEY,
                    TencentCloudRegion.Ids.NA_TORONTO,
            },
            optionNames = {
                    TencentCloudRegion.Names.AP_BEIJING,
                    TencentCloudRegion.Names.AP_GUANGZHOU,
                    TencentCloudRegion.Names.AP_HONGKONG,
                    TencentCloudRegion.Names.AP_SHANGHAI,
                    TencentCloudRegion.Names.AP_SHANGHAI_FSI,
                    TencentCloudRegion.Names.AP_SHENZHEN_FSI,
                    TencentCloudRegion.Names.AP_SINGAPORE,
                    TencentCloudRegion.Names.NA_SILICONVALLEY,
                    TencentCloudRegion.Names.NA_TORONTO,
            }
    )
    private String region;
}
