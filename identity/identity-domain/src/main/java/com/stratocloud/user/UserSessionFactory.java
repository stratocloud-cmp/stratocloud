package com.stratocloud.user;


import com.stratocloud.identity.RoleType;
import com.stratocloud.auth.UserSession;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.group.UserGroup;
import com.stratocloud.group.UserGroupTag;
import com.stratocloud.jpa.entities.Auditable;
import com.stratocloud.permission.Permission;
import com.stratocloud.role.Role;
import com.stratocloud.tenant.Tenant;
import com.stratocloud.repository.TenantRepository;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class UserSessionFactory {

    private final TenantRepository tenantRepository;


    public UserSessionFactory(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public UserSession createUserSession(User user) {
        String token = UUID.randomUUID().toString();
        Map<String, List<String>> permissions = getPermissions(user);
        List<Long> grantedTenantIds = getGrantedTenantIds(user);

        List<Long> subTenantIds = getSubTenantIds(grantedTenantIds);
        List<Long> inheritedTenantIds = getInheritedTenantIds(grantedTenantIds);
        RoleType highestRoleType = getHighestRoleType(user);
        Map<String, List<String>> tags = getUserGroupTags(user);

        return UserSession.builder()
                .userId(user.getId())
                .loginName(user.getLoginName())
                .realName(user.getRealName())
                .lastLoginTime(user.getLastLoginTime())
                .passwordExpired(user.isPasswordExpired())
                .token(token)
                .permissions(permissions)
                .tenantId(user.getTenantId())
                .grantedTenantIds(grantedTenantIds)
                .subTenantIds(subTenantIds)
                .inheritedTenantIds(inheritedTenantIds)
                .roleType(highestRoleType)
                .grantedTags(tags)
                .build();
    }

    private Map<String, List<String>> getUserGroupTags(User user) {
        Map<String, List<String>> tags = new HashMap<>();
        for (UserGroup group : user.getGroups()) {
            for (UserGroupTag tag : group.getTags()) {
                tags.computeIfAbsent(tag.getTagKey(), k -> new ArrayList<>());
                tags.get(tag.getTagKey()).add(tag.getTagValue());
            }
        }
        return tags;
    }

    private RoleType getHighestRoleType(User user) {
        List<UserRole> userRoles = new ArrayList<>(user.getUserRoles());

        if(Utils.isEmpty(userRoles))
            throw new BadCommandException("用户没有角色");

        userRoles.sort(Comparator.comparingInt(r -> r.getRole().getType().ordinal()));
        return userRoles.get(0).getRole().getType();
    }

    private List<Long> getGrantedTenantIds(User user) {
        Set<Long> tenantIds = new HashSet<>();

        tenantIds.add(user.getTenantId());

        List<UserRole> userRoles = user.getUserRoles();
        if(Utils.isEmpty(userRoles))
            return new ArrayList<>(tenantIds);

        for (UserRole userRole : userRoles) 
            if(Utils.isNotEmpty(userRole.getGrantedTenantIds()))
                tenantIds.addAll(userRole.getGrantedTenantIds());
        
        return new ArrayList<>(tenantIds);
    }

    private List<Long> getInheritedTenantIds(Collection<Long> tenantIds) {
        Set<Long> inheritedTenantIds = new HashSet<>();

        for (Long tenantId : tenantIds) {
            List<Tenant> inheritedTenants = tenantRepository.findInheritedTenants(tenantId);
            inheritedTenantIds.addAll(inheritedTenants.stream().map(Auditable::getId).toList());
        }


        return new ArrayList<>(inheritedTenantIds);
    }

    private List<Long> getSubTenantIds(Collection<Long> tenantIds) {
        Set<Long> subTenantIds = new HashSet<>();

        for (Long tenantId : tenantIds) {
            List<Tenant> subTenants = tenantRepository.findSubTenants(tenantId);
            subTenantIds.addAll(subTenants.stream().map(Auditable::getId).toList());
        }


        return new ArrayList<>(subTenantIds);
    }

    private Map<String, List<String>> getPermissions(User user) {
        Map<String, List<String>> permissions = new HashMap<>();
        List<UserRole> userRoles = user.getUserRoles();
        if(Utils.isEmpty(userRoles))
            return permissions;

        for (UserRole userRole : userRoles) {
            Role role = userRole.getRole();
            var permissionEntities = role.getPermissions();
            for (Permission permissionEntity : permissionEntities) {
                permissions.computeIfAbsent(permissionEntity.getTarget(), t -> new ArrayList<>());
                permissions.get(permissionEntity.getTarget()).add(permissionEntity.getAction());
            }
        }

        return permissions;
    }
}
