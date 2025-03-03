package com.stratocloud.provider.aliyun.common;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum AliyunRegion {
    QINGDAO(Ids.CN_QINGDAO, Names.CN_QINGDAO),
    BEIJING(Ids.CN_BEIJING, Names.CN_BEIJING),
    ZHANGJIAKOU(Ids.CN_ZHANGJIAKOU, Names.CN_ZHANGJIAKOU),
    HUHEHAOTE(Ids.CN_HUHEHAOTE, Names.CN_HUHEHAOTE),
    WULANCHABU(Ids.CN_WULANCHABU, Names.CN_WULANCHABU),
    HANGZHOU(Ids.CN_HANGZHOU, Names.CN_HANGZHOU),
    SHANGHAI(Ids.CN_SHANGHAI, Names.CN_SHANGHAI),
    NANJING(Ids.CN_NANJING, Names.CN_NANJING),
    FUZHOU(Ids.CN_FUZHOU, Names.CN_FUZHOU),
    WUHAN(Ids.CN_WUHAN, Names.CN_WUHAN),
    SHENZHEN(Ids.CN_SHENZHEN, Names.CN_SHENZHEN),
    HEYUAN(Ids.CN_HEYUAN, Names.CN_HEYUAN),
    GUANGZHOU(Ids.CN_GUANGZHOU, Names.CN_GUANGZHOU),
    CHENGDU(Ids.CN_CHENGDU, Names.CN_CHENGDU),
    HONGKONG(Ids.CN_HONGKONG, Names.CN_HONGKONG),
    SINGAPORE(Ids.AP_SINGAPORE, Names.AP_SINGAPORE),
    SYDNEY(Ids.AP_SYDNEY, Names.AP_SYDNEY),
    KUALA_LUMPUR(Ids.AP_KUALA_LUMPUR, Names.AP_KUALA_LUMPUR),
    JAKARTA(Ids.AP_JAKARTA, Names.AP_JAKARTA),
    MANILA(Ids.AP_MANILA, Names.AP_MANILA),
    BANGKOK(Ids.AP_BANGKOK, Names.AP_BANGKOK),
    TOKYO(Ids.AP_TOKYO, Names.AP_TOKYO),
    SEOUL(Ids.AP_SEOUL, Names.AP_SEOUL),
    SILICON_VALLEY(Ids.US_SILICON_VALLEY, Names.US_SILICON_VALLEY),
    VIRGINIA(Ids.US_VIRGINIA, Names.US_VIRGINIA),
    FRANKFURT(Ids.EU_FRANKFURT, Names.EU_FRANKFURT),
    LONDON(Ids.EU_LONDON, Names.EU_LONDON),
    DUBAI(Ids.ME_DUBAI, Names.ME_DUBAI),
    RIYADH(Ids.ME_RIYADH, Names.ME_RIYADH),
    ;


    private final String id;
    private final String name;
    AliyunRegion(String id, String name){
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }



    public static Optional<AliyunRegion> fromId(String regionId){
        return Arrays.stream(AliyunRegion.values()).filter(
                r -> Objects.equals(regionId, r.getId())
        ).findAny();
    }

    public static class Ids {
        public static final String CN_QINGDAO = "cn-qingdao";
        public static final String CN_BEIJING = "cn-beijing";
        public static final String CN_ZHANGJIAKOU = "cn-zhangjiakou";
        public static final String CN_HUHEHAOTE = "cn-huhehaote";
        public static final String CN_WULANCHABU = "cn-wulanchabu";
        public static final String CN_HANGZHOU = "cn-hangzhou";
        public static final String CN_SHANGHAI = "cn-shanghai";
        public static final String CN_NANJING = "cn-nanjing";
        public static final String CN_FUZHOU = "cn-fuzhou";
        public static final String CN_WUHAN = "cn-wuhan-lr";
        public static final String CN_SHENZHEN = "cn-shenzhen";
        public static final String CN_HEYUAN = "cn-heyuan";
        public static final String CN_GUANGZHOU = "cn-guangzhou";
        public static final String CN_CHENGDU = "cn-chengdu";
        public static final String CN_HONGKONG = "cn-hongkong";
        public static final String AP_SINGAPORE = "ap-southeast-1";
        public static final String AP_SYDNEY = "ap-southeast-2";
        public static final String AP_KUALA_LUMPUR = "ap-southeast-3";
        public static final String AP_JAKARTA = "ap-southeast-5";
        public static final String AP_MANILA = "ap-southeast-6";
        public static final String AP_BANGKOK = "ap-southeast-7";
        public static final String AP_TOKYO = "ap-northeast-1";
        public static final String AP_SEOUL= "ap-northeast-2";
        public static final String US_SILICON_VALLEY = "us-west-1";
        public static final String US_VIRGINIA = "us-east-1";
        public static final String EU_FRANKFURT = "eu-central-1";
        public static final String EU_LONDON = "eu-west-1";
        public static final String ME_DUBAI = "me-east-1";
        public static final String ME_RIYADH = "me-central-1";
    }

    public static class Names {
        public static final String CN_QINGDAO = "华北1（青岛）";
        public static final String CN_BEIJING = "华北2（北京）";
        public static final String CN_ZHANGJIAKOU = "华北3（张家口）";
        public static final String CN_HUHEHAOTE = "华北5（呼和浩特）";
        public static final String CN_WULANCHABU = "华北6（乌兰察布）";
        public static final String CN_HANGZHOU = "华东1（杭州）";
        public static final String CN_SHANGHAI = "华东2（上海）";
        public static final String CN_NANJING = "华东5 （南京-本地地域）";
        public static final String CN_FUZHOU = "华东6（福州-本地地域）";
        public static final String CN_WUHAN = "华中1（武汉-本地地域）";
        public static final String CN_SHENZHEN = "华南1（深圳）";
        public static final String CN_HEYUAN = "华南2（河源）";
        public static final String CN_GUANGZHOU = "华南3（广州）";
        public static final String CN_CHENGDU = "西南1（成都）";
        public static final String CN_HONGKONG = "中国香港";
        public static final String AP_SINGAPORE = "新加坡";
        public static final String AP_SYDNEY = "澳大利亚（悉尼）";
        public static final String AP_KUALA_LUMPUR = "马来西亚（吉隆坡）";
        public static final String AP_JAKARTA = "印度尼西亚（雅加达）";
        public static final String AP_MANILA = "菲律宾（马尼拉）";
        public static final String AP_BANGKOK = "泰国（曼谷）";
        public static final String AP_TOKYO = "日本（东京）";
        public static final String AP_SEOUL = "韩国（首尔）";
        public static final String US_SILICON_VALLEY = "美国（硅谷）";
        public static final String US_VIRGINIA = "美国（弗吉尼亚）";
        public static final String EU_FRANKFURT = "德国（法兰克福）";
        public static final String EU_LONDON = "英国（伦敦）";
        public static final String ME_DUBAI = "阿联酋（迪拜）";
        public static final String ME_RIYADH = "沙特（利雅得）";
    }
}
