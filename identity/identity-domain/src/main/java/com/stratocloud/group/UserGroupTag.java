package com.stratocloud.group;

import com.stratocloud.jpa.entities.Auditable;
import com.stratocloud.tag.NestedTag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserGroupTag extends Auditable implements NestedTag {
    @ManyToOne
    private UserGroup group;

    @Column(nullable = false)
    private String tagKey;

    @Column(nullable = false)
    private String tagKeyName;

    @Column(nullable = false)
    private String tagValue;

    @Column(nullable = false)
    private String tagValueName;

    public UserGroupTag(String tagKey, String tagKeyName, String tagValue, String tagValueName) {
        this.tagKey = tagKey;
        this.tagKeyName = tagKeyName;
        this.tagValue = tagValue;
        this.tagValueName = tagValueName;
    }
}
