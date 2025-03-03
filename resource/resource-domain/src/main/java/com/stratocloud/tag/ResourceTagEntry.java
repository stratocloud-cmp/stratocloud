package com.stratocloud.tag;


import com.stratocloud.exceptions.StratoException;
import com.stratocloud.jpa.entities.Tenanted;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Entity(name = "resource_tag_entry")
@Table(
        name = "resource_tag_entry",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_idx_tag_key",
                columnNames = "tag_key"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceTagEntry extends Tenanted {
    @Column
    private String resourceCategory;
    @Column(name = "tag_key", nullable = false)
    private String tagKey;
    @Column(nullable = false)
    private String tagKeyName;

    @Column
    private String description;

    @Column
    private Boolean disabled = false;

    @Column(nullable = false)
    private Boolean requiredWhenCreating;
    @Column(nullable = false)
    private Boolean requiredWhenFiltering;


    @Column(nullable = false)
    private Boolean userGroupTaggable;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "entry", orphanRemoval = true)
    private List<ResourceTagValue> values = new ArrayList<>();

    @Builder
    public ResourceTagEntry(String resourceCategory, String tagKey, String tagKeyName, String description,
                            Boolean requiredWhenCreating, Boolean requiredWhenFiltering,
                            Boolean userGroupTaggable) {
        this.resourceCategory = resourceCategory;
        this.tagKey = tagKey;
        this.tagKeyName = tagKeyName;
        this.description = description;
        this.requiredWhenCreating = requiredWhenCreating;
        this.requiredWhenFiltering = requiredWhenFiltering;
        this.userGroupTaggable = userGroupTaggable;
    }

    public void update(String resourceCategory, String tagKeyName,
                       String description, Boolean requiredWhenCreating, Boolean requiredWhenFiltering,
                       Boolean userGroupTaggable) {
        this.resourceCategory = resourceCategory;
        this.tagKeyName = tagKeyName;
        this.description = description;
        this.requiredWhenCreating = requiredWhenCreating;
        this.requiredWhenFiltering = requiredWhenFiltering;
        this.userGroupTaggable = userGroupTaggable;
    }

    public void disable(){
        this.disabled = true;
    }

    public void enable(){
        this.disabled = false;
    }

    public ResourceTagValue getTagValueById(Long tagValueId) {
        return values.stream().filter(
                v -> Objects.equals(tagValueId, v.getId())
        ).findAny().orElseThrow(
                () -> new StratoException("Tag value not found")
        );
    }

    public void addValue(String tagValue, String tagValueName, int index, String description) {
        if(values.stream().anyMatch(v->Objects.equals(tagValue, v.getTagValue())))
            return;

        ResourceTagValue value = new ResourceTagValue(this, tagValue, tagValueName, index, description);
        values.add(value);
    }

    public void removeValueById(Long tagValueId) {
        ResourceTagValue tagValue = getTagValueById(tagValueId);

        values.remove(tagValue);
    }
}
