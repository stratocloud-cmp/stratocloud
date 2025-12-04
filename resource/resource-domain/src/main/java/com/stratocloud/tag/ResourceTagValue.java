package com.stratocloud.tag;


import com.stratocloud.jpa.entities.Tenanted;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(
        name = "resource_tag_value",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_idx_tag_value",
                columnNames = {"entry_id", "tag_value"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceTagValue extends Tenanted {
    @ManyToOne
    private ResourceTagEntry entry;
    @Column
    @ElementCollection
    private Set<String> resourceTypes = new HashSet<>();
    @Column(nullable = false)
    private String tagValue;
    @Column(nullable = false)
    private String tagValueName;
    @Column(nullable = false)
    private Integer index;
    @Column
    private String description;


    public ResourceTagValue(ResourceTagEntry entry,
                            Set<String> resourceTypes,
                            String tagValue,
                            String tagValueName,
                            Integer index,
                            String description) {
        this.resourceTypes.addAll(resourceTypes);
        this.entry = entry;
        this.tagValue = tagValue;
        this.tagValueName = tagValueName;
        this.index = index;
        this.description = description;
    }


}
