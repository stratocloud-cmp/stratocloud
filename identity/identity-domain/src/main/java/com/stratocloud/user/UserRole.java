package com.stratocloud.user;

import com.stratocloud.jpa.entities.Auditable;
import com.stratocloud.role.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRole extends Auditable {
    @ManyToOne
    private User user;
    @ManyToOne
    private Role role;
    @Column
    private List<Long> grantedTenantIds = new ArrayList<>();

    public UserRole(User user, Role role, List<Long> grantedTenantIds) {
        this.user = user;
        this.role = role;
        this.grantedTenantIds = grantedTenantIds;
    }
}
