package com.stratocloud.provider.huawei.common;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum HuaweiCloudRegion {
    SHANGHAI_2(Ids.CN_EAST_2, Names.CN_EAST_2),
    SHANGHAI_1(Ids.CN_EAST_3, Names.CN_EAST_3),
    CN_EAST_4(Ids.CN_EAST_4, Names.CN_EAST_4),
    QINGDAO(Ids.CN_EAST_5, Names.CN_EAST_5),
    BEIJING_1(Ids.CN_NORTH_1, Names.CN_NORTH_1),
    BEIJING_2(Ids.CN_NORTH_2, Names.CN_NORTH_2),
    BEIJING_4(Ids.CN_NORTH_4, Names.CN_NORTH_4),
    WULANCHABU(Ids.CN_NORTH_9, Names.CN_NORTH_9),
    GUANGZHOU(Ids.CN_SOUTH_1, Names.CN_SOUTH_1),
    SHENZHEN(Ids.CN_SOUTH_2, Names.CN_SOUTH_2),
    GUANGZHOU_2(Ids.CN_SOUTH_4, Names.CN_SOUTH_4),
    GUIYANG(Ids.CN_SOUTHWEST_2, Names.CN_SOUTHWEST_2),
    ABU_DHABI(Ids.AE_AD_1, Names.AE_AD_1),
    CAIRO(Ids.AF_NORTH_1, Names.AF_NORTH_1),
    JOHANNESBURG(Ids.AF_SOUTH_1, Names.AF_SOUTH_1),
    HONGKONG(Ids.AP_SOUTHEAST_1, Names.AP_SOUTHEAST_1),
    BANGKOK(Ids.AP_SOUTHEAST_2, Names.AP_SOUTHEAST_2),
    SINGAPORE(Ids.AP_SOUTHEAST_3, Names.AP_SOUTHEAST_3),
    JAKARTA(Ids.AP_SOUTHEAST_4, Names.AP_SOUTHEAST_4),
    MANILA(Ids.AP_SOUTHEAST_5, Names.AP_SOUTHEAST_5),
    PARIS(Ids.EU_WEST_0, Names.EU_WEST_0),
    MEXICO_2(Ids.LA_NORTH_2, Names.LA_NORTH_2),
    SANTIAGO(Ids.LA_SOUTH_2, Names.LA_SOUTH_2),
    RIYADH(Ids.ME_EAST_1, Names.ME_EAST_1),
    KUALA_LUMPUR(Ids.MY_KUALA_LUMPUR_1, Names.MY_KUALA_LUMPUR_1),
    MEXICO_1(Ids.NA_MEXICO_1, Names.NA_MEXICO_1),
    MOSCOW(Ids.RU_MOSCOW_1, Names.RU_MOSCOW_1),
    SAO_PAULO(Ids.SA_BRAZIL_1, Names.SA_BRAZIL_1),
    ISTANBUL(Ids.TR_WEST_1, Names.TR_WEST_1),
    ;


    private final String id;
    private final String name;
    HuaweiCloudRegion(String id, String name){
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }



    public static Optional<HuaweiCloudRegion> fromId(String regionId){
        return Arrays.stream(HuaweiCloudRegion.values()).filter(
                r -> Objects.equals(regionId, r.getId())
        ).findAny();
    }

    public static class Ids {
        public static final String CN_EAST_2 = "cn-east-2";
        public static final String CN_EAST_3 = "cn-east-3";
        public static final String CN_EAST_4 = "cn-east-4";
        public static final String CN_EAST_5 = "cn-east-5";
        public static final String CN_NORTH_1 = "cn-north-1";
        public static final String CN_NORTH_2 = "cn-north-2";
        public static final String CN_NORTH_4 = "cn-north-4";
        public static final String CN_NORTH_9 = "cn-north-9";
        public static final String CN_SOUTH_1 = "cn-south-1";
        public static final String CN_SOUTH_2 = "cn-south-2";
        public static final String CN_SOUTH_4 = "cn-south-4";
        public static final String CN_SOUTHWEST_2 = "cn-southwest-2";
        public static final String AE_AD_1 = "ae-ad-1";
        public static final String AF_NORTH_1 = "af-north-1";
        public static final String AF_SOUTH_1 = "af-south-1";
        public static final String AP_SOUTHEAST_1 = "ap-southeast-1";
        public static final String AP_SOUTHEAST_2 = "ap-southeast-2";
        public static final String AP_SOUTHEAST_3 = "ap-southeast-3";
        public static final String AP_SOUTHEAST_4 = "ap-southeast-4";
        public static final String AP_SOUTHEAST_5 = "ap-southeast-5";
        public static final String EU_WEST_0 = "eu-west-0";
        public static final String LA_NORTH_2 = "la-north-2";
        public static final String LA_SOUTH_2 = "la-south-2";
        public static final String ME_EAST_1 = "me-east-1";
        public static final String MY_KUALA_LUMPUR_1 = "my-kualalumpur-1";
        public static final String NA_MEXICO_1 = "na-mexico-1";
        public static final String RU_MOSCOW_1 = "ru-moscow-1";
        public static final String SA_BRAZIL_1 = "sa-brazil-1";
        public static final String TR_WEST_1 = "tr-west-1";
    }

    public static class Names {
        public static final String CN_EAST_2 = "华东-上海二";
        public static final String CN_EAST_3 = "华东-上海一";
        public static final String CN_EAST_4 = "华东二";
        public static final String CN_EAST_5 = "华东-青岛";
        public static final String CN_NORTH_1 = "华北-北京一";
        public static final String CN_NORTH_2 = "华北-北京二";
        public static final String CN_NORTH_4 = "华北-北京四";
        public static final String CN_NORTH_9 = "华北-乌兰察布一";
        public static final String CN_SOUTH_1 = "华南-广州";
        public static final String CN_SOUTH_2 = "华南-深圳";
        public static final String CN_SOUTH_4 = "华南-广州-友好用户环境";
        public static final String CN_SOUTHWEST_2 = "西南-贵阳一";
        public static final String AE_AD_1 = "中东-阿布扎比-OP5";
        public static final String AF_NORTH_1 = "非洲-开罗";
        public static final String AF_SOUTH_1 = "非洲-约翰内斯堡";
        public static final String AP_SOUTHEAST_1 = "中国-香港";
        public static final String AP_SOUTHEAST_2 = "亚太-曼谷";
        public static final String AP_SOUTHEAST_3 = "亚太-新加坡";
        public static final String AP_SOUTHEAST_4 = "亚太-雅加达";
        public static final String AP_SOUTHEAST_5 = "亚太-马尼拉";
        public static final String EU_WEST_0 = "欧洲-巴黎";
        public static final String LA_NORTH_2 = "拉美-墨西哥城二";
        public static final String LA_SOUTH_2 = "拉美-圣地亚哥";
        public static final String ME_EAST_1 = "中东-利雅得";
        public static final String MY_KUALA_LUMPUR_1 = "亚太-吉隆坡-OP6";
        public static final String NA_MEXICO_1 = "拉美-墨西哥城一";
        public static final String RU_MOSCOW_1 = "俄罗斯-莫斯科-OP4";
        public static final String SA_BRAZIL_1 = "拉美-圣保罗一";
        public static final String TR_WEST_1 = "土耳其-伊斯坦布尔";
    }
}
