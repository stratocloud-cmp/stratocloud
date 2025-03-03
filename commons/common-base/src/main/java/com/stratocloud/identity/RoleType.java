package com.stratocloud.identity;

/**
 * SUPER_ADMIN has all data permissions and operation permissions.
 * TENANT_SUPER_ADMIN has limited data permissions compare to SUPER_ADMIN.
 * TENANT_ADMIN has limited operation permissions compare to TENANT_SUPER_ADMIN.
 * NORMAL_USER can only operate controllable entities.
 */
public enum RoleType {
    SUPER_ADMIN,
    TENANT_SUPER_ADMIN,
    TENANT_ADMIN,
    NORMAL_USER
}
