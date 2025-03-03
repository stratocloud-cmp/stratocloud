package com.stratocloud.provider;

import com.stratocloud.provider.constants.CpuArch;
import com.stratocloud.provider.constants.TagEntries;
import com.stratocloud.tag.Tag;

public class TagFactory {
    public static Tag buildFlavorFamilyTag(String familyId, String familyName, int index){
        return new Tag(TagEntries.FLAVOR_FAMILY, familyId, familyName, index);
    }

    public static Tag buildFlavorSizeTag(int cpuCores, Number memoryGb){
        String flavorSize = "%sC%sG".formatted(cpuCores, memoryGb.intValue());
        int flavorIndex = cpuCores * 10000 + memoryGb.intValue();
        return new Tag(TagEntries.FLAVOR_SIZE, flavorSize, flavorSize, flavorIndex);
    }

    public static Tag buildImagePlatformTag(String platform) {
        return new Tag(TagEntries.IMAGE_PLATFORM, platform, platform, 1);
    }

    public static Tag buildCpuArchTag(CpuArch cpuArch) {
        return new Tag(TagEntries.CPU_ARCH, cpuArch.name(), cpuArch.name(), 0);
    }
}
