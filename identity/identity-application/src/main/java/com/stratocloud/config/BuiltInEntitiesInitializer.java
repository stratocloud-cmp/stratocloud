package com.stratocloud.config;

import com.stratocloud.auth.CallContext;
import com.stratocloud.identity.RoleType;
import com.stratocloud.identity.BuiltInAuthTypes;
import com.stratocloud.identity.BuiltInIds;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.repository.UserRepository;
import com.stratocloud.role.Role;
import com.stratocloud.repository.RoleRepository;
import com.stratocloud.tenant.Tenant;
import com.stratocloud.repository.TenantRepository;
import com.stratocloud.user.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class BuiltInEntitiesInitializer implements InitializingBean {
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    private final DefaultUserAuthenticator defaultUserAuthenticator;

    public BuiltInEntitiesInitializer(TenantRepository tenantRepository,
                                      RoleRepository roleRepository,
                                      UserRepository userRepository,
                                      DefaultUserAuthenticator defaultUserAuthenticator) {
        this.tenantRepository = tenantRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.defaultUserAuthenticator = defaultUserAuthenticator;
    }


    @Override
    public void afterPropertiesSet() {
        CallContext.registerSystemSession();

        Tenant rootTenant = createRootTenant();

        if(tenantRepository.existsById(rootTenant.getId()))
            log.info("Root Tenant already exists.");
        else
            tenantRepository.save(rootTenant);

        Role superAdmin = createSuperAdmin();

        if(roleRepository.existsById(superAdmin.getId()))
            log.info("Super admin already exists.");
        else
            superAdmin = roleRepository.save(superAdmin);

        User systemUser = createSystemUser(superAdmin);

        if(userRepository.existsById(systemUser.getId()))
            log.info("System user already exists.");
        else
            userRepository.save(systemUser);

        User superAdminUser = createSuperAdminUser(superAdmin);

        if(userRepository.existsById(superAdminUser.getId()))
            log.info("Super admin user already exists");
        else
            userRepository.save(superAdminUser);
    }

    private User createSuperAdminUser(Role superAdmin) {
        User user = new User(
                BuiltInIds.ROOT_TENANT_ID,
                "super_admin",
                "超级管理员",
                null,
                null,
                getDefaultPassword(),
                null,
                null,
                BuiltInAuthTypes.DEFAULT_AUTH_TYPE
        );

        EntityUtil.forceSetId(user, BuiltInIds.SUPER_ADMIN_USER_ID);

        user.assignRole(superAdmin, List.of(BuiltInIds.ROOT_TENANT_ID));

        return user;
    }

    private EncodedPassword getDefaultPassword() {
        Password password = defaultUserAuthenticator.preEncodePassword("super_admin");
        return defaultUserAuthenticator.encodePassword(password);
    }

    private User createSystemUser(Role superAdmin) {
        User user = new User(
                BuiltInIds.ROOT_TENANT_ID,
                "system",
                "系统",
                null,
                null,
                null,
                null,
                null,
                BuiltInAuthTypes.NONE
        );

        EntityUtil.forceSetId(user, BuiltInIds.SYSTEM_USER_ID);

        user.assignRole(superAdmin, List.of(BuiltInIds.ROOT_TENANT_ID));

        return user;
    }

    private Role createSuperAdmin() {
        Role role = new Role(RoleType.SUPER_ADMIN, "超级管理员", null);
        EntityUtil.forceSetId(role, BuiltInIds.SUPER_ADMIN_ROLE_ID);
        role.setTenantId(BuiltInIds.ROOT_TENANT_ID);
        return role;
    }

    private Tenant createRootTenant() {
        Tenant tenant = new Tenant("默认租户", null);
        EntityUtil.forceSetId(tenant, BuiltInIds.ROOT_TENANT_ID);
        return tenant;
    }
}
