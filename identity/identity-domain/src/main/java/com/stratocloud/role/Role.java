package com.stratocloud.role;

import com.stratocloud.identity.RoleType;
import com.stratocloud.jpa.entities.Tenanted;
import com.stratocloud.permission.Permission;
import com.stratocloud.user.UserRole;
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
public class Role extends Tenanted {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleType type;
    @Column(nullable = false)
    private String name;
    @Column
    private String description;
    @ManyToMany
    private List<Permission> permissions = new ArrayList<>();
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "role", orphanRemoval = true)
    private List<UserRole> userRoles = new ArrayList<>();


    public Role(RoleType type, String name, String description) {
        this.type = type;
        this.name = name;
        this.description = description;
    }

    public void update(RoleType roleType, String name, String description) {
        this.type = roleType;
        this.name = name;
        this.description = description;
    }

    public void addPermissions(List<Permission> permissions) {
        for (Permission permission : permissions) {
            if(hasPermission(permission))
                continue;

            this.permissions.add(permission);
        }
    }

    private boolean hasPermission(Permission permission) {
        return permissions.stream().anyMatch(p -> p.getId().equals(permission.getId()));
    }

    public void removePermissions(List<Long> permissionIds) {
        permissions.removeIf(p -> permissionIds.contains(p.getId()));
    }
}
