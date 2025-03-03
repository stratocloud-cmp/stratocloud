package com.stratocloud.user;

import com.stratocloud.group.UserGroup;
import com.stratocloud.identity.IdentityTopics;
import com.stratocloud.identity.SimpleUser;
import com.stratocloud.identity.UserDeletedPayload;
import com.stratocloud.jpa.entities.Tenanted;
import com.stratocloud.messaging.Message;
import com.stratocloud.role.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "strato_user")
public class User extends Tenanted {
    @Column(nullable = false)
    private String loginName;
    @Column(nullable = false)
    private String realName;
    @Column
    private String emailAddress;
    @Column
    private String phoneNumber;
    @Column
    @Convert(converter = EncodedPasswordConverter.class)
    private EncodedPassword password;
    @Column
    private Long iconId;
    @Column
    private String description;
    @Column(nullable = false)
    private String authType;
    @Column(nullable = false)
    private Boolean disabled = false;
    @Column(nullable = false)
    private Boolean locked = false;
    @Column(nullable = false)
    private Integer failedLoginAttempts = 0;
    @Column
    private LocalDateTime lastLoginTime;
    @Column
    private LocalDateTime passwordExpireTime;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
    private List<UserRole> userRoles = new ArrayList<>();

    @ManyToMany(mappedBy = "members")
    private List<UserGroup> groups = new ArrayList<>();

    public User(Long tenantId, String loginName, String realName, String emailAddress,
                String phoneNumber, EncodedPassword password, Long iconId,
                String description, String authType) {
        setTenantId(tenantId);
        this.loginName = loginName;
        this.realName = realName;
        this.emailAddress = emailAddress;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.iconId = iconId;
        this.description = description;
        this.authType = authType;
    }

    public void update(String realName, String emailAddress, String phoneNumber, String description) {
        this.realName = realName;
        this.emailAddress = emailAddress;
        this.phoneNumber = phoneNumber;
        this.description = description;
    }

    public void disable() {
        this.disabled = true;
    }

    public void enable() {
        this.disabled = false;
    }

    private void lock() {
        this.locked = true;
    }

    public void unlock() {
        this.locked = false;
        resetLoginAttempts();
    }

    public void resetLoginAttempts(){
        this.failedLoginAttempts = 0;
    }

    public void onLogin() {
        this.lastLoginTime = LocalDateTime.now();
    }

    public void onLoginFailed(int maxLoginAttempts) {
        this.failedLoginAttempts++;
        if(this.failedLoginAttempts>=maxLoginAttempts)
            lock();
    }

    public boolean isPasswordExpired() {
        if(passwordExpireTime == null)
            return false;
        return LocalDateTime.now().isAfter(passwordExpireTime);
    }

    public void assignRole(Role role, List<Long> grantedTenantIds) {
        UserRole userRole = new UserRole(this, role, grantedTenantIds);
        userRoles.add(userRole);
    }

    public void removeRoleById(Long roleId) {
        userRoles.removeIf(userRole -> userRole.getRole().getId().equals(roleId));
    }

    public void onDelete() {
        SimpleUser simpleUser = new SimpleUser(getId(), loginName, realName);
        UserDeletedPayload payload = new UserDeletedPayload(simpleUser);
        publish(Message.create(IdentityTopics.USER_DELETED_TOPIC, payload));
    }

    public void changePassword(EncodedPassword encodedPassword) {
        setPassword(encodedPassword);
    }
}
