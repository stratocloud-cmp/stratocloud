package com.stratocloud.resource;

import com.stratocloud.jpa.entities.Auditable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class ResourceTag extends Auditable {
    @ManyToOne
    private Resource resource;

    @Column(nullable = false)
    private String tagKey;

    @Column(nullable = false)
    private String tagKeyName;

    @Column(nullable = false)
    private String tagValue;

    @Column(nullable = false)
    private String tagValueName;

    public ResourceTag(String tagKey,
                       String tagKeyName,
                       String tagValue,
                       String tagValueName) {
        this.tagKey = tagKey;
        this.tagKeyName = tagKeyName;
        this.tagValue = tagValue;
        this.tagValueName = tagValueName;
    }

    public static ResourceTag copyOf(ResourceTag tag){
        return new ResourceTag(tag.getTagKey(), tag.getTagKeyName(), tag.getTagValue(), tag.getTagValueName());
    }
}
