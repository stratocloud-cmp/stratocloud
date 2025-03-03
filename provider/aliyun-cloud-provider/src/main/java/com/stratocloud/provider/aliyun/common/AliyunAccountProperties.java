package com.stratocloud.provider.aliyun.common;

import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.ExternalAccountProperties;
import lombok.Data;

@Data
public class AliyunAccountProperties implements ExternalAccountProperties {
    @InputField(label = "AccessKeyId", description = "API密钥的AccessKeyId", inputType = "textarea")
    private String accessKeyId;
    @InputField(label = "AccessKeySecret", description = "API密钥的AccessKeySecret", inputType = "textarea")
    private String accessKeySecret;
    @SelectField(
            label = "地域",
            allowCreate = true,
            description = "若所需地域在选项中不存在，可直接输入其地域ID。",
            options = {
                    AliyunRegion.Ids.CN_QINGDAO,
                    AliyunRegion.Ids.CN_BEIJING,
                    AliyunRegion.Ids.CN_ZHANGJIAKOU,
                    AliyunRegion.Ids.CN_HUHEHAOTE,
                    AliyunRegion.Ids.CN_WULANCHABU,
                    AliyunRegion.Ids.CN_HANGZHOU,
                    AliyunRegion.Ids.CN_SHANGHAI,
                    AliyunRegion.Ids.CN_NANJING,
                    AliyunRegion.Ids.CN_FUZHOU,
                    AliyunRegion.Ids.CN_WUHAN,
                    AliyunRegion.Ids.CN_SHENZHEN,
                    AliyunRegion.Ids.CN_HEYUAN,
                    AliyunRegion.Ids.CN_GUANGZHOU,
                    AliyunRegion.Ids.CN_CHENGDU,
                    AliyunRegion.Ids.CN_HONGKONG,
                    AliyunRegion.Ids.AP_SINGAPORE,
                    AliyunRegion.Ids.AP_SYDNEY,
                    AliyunRegion.Ids.AP_KUALA_LUMPUR,
                    AliyunRegion.Ids.AP_JAKARTA,
                    AliyunRegion.Ids.AP_MANILA,
                    AliyunRegion.Ids.AP_BANGKOK,
                    AliyunRegion.Ids.AP_TOKYO,
                    AliyunRegion.Ids.AP_SEOUL,
                    AliyunRegion.Ids.US_SILICON_VALLEY,
                    AliyunRegion.Ids.US_VIRGINIA,
                    AliyunRegion.Ids.EU_FRANKFURT,
                    AliyunRegion.Ids.EU_LONDON,
                    AliyunRegion.Ids.ME_DUBAI,
                    AliyunRegion.Ids.ME_RIYADH,
            },
            optionNames = {
                    AliyunRegion.Names.CN_QINGDAO,
                    AliyunRegion.Names.CN_BEIJING,
                    AliyunRegion.Names.CN_ZHANGJIAKOU,
                    AliyunRegion.Names.CN_HUHEHAOTE,
                    AliyunRegion.Names.CN_WULANCHABU,
                    AliyunRegion.Names.CN_HANGZHOU,
                    AliyunRegion.Names.CN_SHANGHAI,
                    AliyunRegion.Names.CN_NANJING,
                    AliyunRegion.Names.CN_FUZHOU,
                    AliyunRegion.Names.CN_WUHAN,
                    AliyunRegion.Names.CN_SHENZHEN,
                    AliyunRegion.Names.CN_HEYUAN,
                    AliyunRegion.Names.CN_GUANGZHOU,
                    AliyunRegion.Names.CN_CHENGDU,
                    AliyunRegion.Names.CN_HONGKONG,
                    AliyunRegion.Names.AP_SINGAPORE,
                    AliyunRegion.Names.AP_SYDNEY,
                    AliyunRegion.Names.AP_KUALA_LUMPUR,
                    AliyunRegion.Names.AP_JAKARTA,
                    AliyunRegion.Names.AP_MANILA,
                    AliyunRegion.Names.AP_BANGKOK,
                    AliyunRegion.Names.AP_TOKYO,
                    AliyunRegion.Names.AP_SEOUL,
                    AliyunRegion.Names.US_SILICON_VALLEY,
                    AliyunRegion.Names.US_VIRGINIA,
                    AliyunRegion.Names.EU_FRANKFURT,
                    AliyunRegion.Names.EU_LONDON,
                    AliyunRegion.Names.ME_DUBAI,
                    AliyunRegion.Names.ME_RIYADH,
            }
    )
    private String region;
}
