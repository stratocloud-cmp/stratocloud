package com.stratocloud.provider.aliyun.disk;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum AliyunDiskCategory {
    cloud(Ids.CLOUD, Names.CLOUD),
    cloud_efficiency(Ids.CLOUD_EFFICIENCY, Names.CLOUD_EFFICIENCY),
    cloud_ssd(Ids.CLOUD_SSD, Names.CLOUD_SSD),
    cloud_essd(Ids.CLOUD_ESSD, Names.CLOUD_ESSD),
    cloud_auto(Ids.CLOUD_AUTO, Names.CLOUD_AUTO),
    local_ssd_pro(Ids.LOCAL_SSD_PRO, Names.LOCAL_SSD_PRO),
    local_hdd_pro(Ids.LOCAL_HDD_PRO, Names.LOCAL_HDD_PRO),
    cloud_essd_entry(Ids.CLOUD_ESSD_ENTRY, Names.CLOUD_ESSD_ENTRY),
    elastic_ephemeral_disk_standard(Ids.ELASTIC_EPHEMERAL_DISK_STANDARD, Names.ELASTIC_EPHEMERAL_DISK_STANDARD),
    elastic_ephemeral_disk_premium(Ids.ELASTIC_EPHEMERAL_DISK_PREMIUM, Names.ELASTIC_EPHEMERAL_DISK_PREMIUM),
    ephemeral(Ids.EPHEMERAL, Names.EPHEMERAL),
    ephemeral_ssd(Ids.EPHEMERAL_SSD, Names.EPHEMERAL_SSD),
    ;


    private final String id;
    private final String name;
    AliyunDiskCategory(String id, String name){
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }



    public static Optional<AliyunDiskCategory> fromId(String diskType){
        return Arrays.stream(AliyunDiskCategory.values()).filter(
                r -> Objects.equals(diskType, r.getId())
        ).findAny();
    }

    public static class Ids {

        public static final String CLOUD = "cloud";

        public static final String CLOUD_EFFICIENCY = "cloud_efficiency";
        public static final String CLOUD_SSD = "cloud_ssd";
        public static final String CLOUD_ESSD = "cloud_essd";
        public static final String CLOUD_AUTO = "cloud_auto";
        public static final String LOCAL_SSD_PRO = "local_ssd_pro";
        public static final String LOCAL_HDD_PRO = "local_hdd_pro";
        public static final String CLOUD_ESSD_ENTRY = "cloud_essd_entry";
        public static final String ELASTIC_EPHEMERAL_DISK_STANDARD = "elastic_ephemeral_disk_standard";
        public static final String ELASTIC_EPHEMERAL_DISK_PREMIUM = "elastic_ephemeral_disk_premium";
        public static final String EPHEMERAL = "ephemeral";
        public static final String EPHEMERAL_SSD = "ephemeral_ssd";
    }

    public static class Names {

        public static final String CLOUD = "普通云盘";

        public static final String CLOUD_EFFICIENCY = "高效云盘";
        public static final String CLOUD_SSD = "SSD云盘";
        public static final String CLOUD_ESSD = "ESSD云盘";
        public static final String CLOUD_AUTO = "ESSD AutoPL 云盘";
        public static final String LOCAL_SSD_PRO = "本地SSD云盘";
        public static final String LOCAL_HDD_PRO = "本地HDD云盘";
        public static final String CLOUD_ESSD_ENTRY = "ESSD Entry 云盘";
        public static final String ELASTIC_EPHEMERAL_DISK_STANDARD = "弹性临时盘-标准版";
        public static final String ELASTIC_EPHEMERAL_DISK_PREMIUM = "弹性临时盘-高级版";
        public static final String EPHEMERAL = "临时盘";
        public static final String EPHEMERAL_SSD = "SSD临时盘";
    }
}
