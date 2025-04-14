package com.stratocloud.tag;

import com.stratocloud.utils.Utils;

import java.util.List;

public record TagRecord(String tagKey,
                        String tagKeyName,
                        String tagValue,
                        String tagValueName) {

    public static List<TagRecord> fromNestedTags(List<NestedTag> nestedTags){
        if(Utils.isEmpty(nestedTags))
            return List.of();

        return nestedTags.stream().map(
                t -> new TagRecord(
                        t.getTagKey(),
                        t.getTagKeyName(),
                        t.getTagValue(),
                        t.getTagValueName()
                )
        ).toList();
    }
}
