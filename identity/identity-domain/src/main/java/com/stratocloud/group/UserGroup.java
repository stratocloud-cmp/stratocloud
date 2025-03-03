package com.stratocloud.group;

import com.stratocloud.jpa.entities.Tenanted;
import com.stratocloud.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserGroup extends Tenanted {
    @Column
    private String name;
    @Column
    private String alias;
    @Column
    private String description;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "group")
    private List<UserGroupTag> tags = new ArrayList<>();
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<User> members = new ArrayList<>();

    public UserGroup(String name, String alias, String description) {
        this.name = name;
        this.alias = alias;
        this.description = description;
    }

    public void addMember(User user){
        if(members.stream().anyMatch(m -> m.getId().equals(user.getId())))
            return;

        this.members.add(user);
        user.getGroups().add(this);
    }

    public void updateTags(List<UserGroupTag> tags) {
        tags.forEach(t->t.setGroup(this));
        this.tags.clear();
        this.tags.addAll(tags);
    }

    public void update(String name, String alias, String description) {
        this.name = name;
        this.alias = alias;
        this.description = description;
    }

    public void removeMemberById(Long userId) {
        members.removeIf(m->m.getId().equals(userId));
    }
}
