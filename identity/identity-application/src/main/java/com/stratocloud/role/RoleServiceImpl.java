package com.stratocloud.role;

import com.stratocloud.audit.AuditLogContext;
import com.stratocloud.audit.AuditObject;
import com.stratocloud.identity.RoleType;
import com.stratocloud.permission.Permission;
import com.stratocloud.repository.PermissionRepository;
import com.stratocloud.repository.RoleRepository;
import com.stratocloud.role.cmd.*;
import com.stratocloud.role.query.*;
import com.stratocloud.role.response.*;
import com.stratocloud.validate.ValidateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository repository;

    private final PermissionRepository permissionRepository;
    private final RoleAssembler assembler;

    public RoleServiceImpl(RoleRepository repository,
                           PermissionRepository permissionRepository,
                           RoleAssembler assembler) {
        this.repository = repository;
        this.permissionRepository = permissionRepository;
        this.assembler = assembler;
    }

    @Override
    @Transactional
    @ValidateRequest
    public CreateRoleResponse createRole(CreateRoleCmd cmd) {
        Long tenantId = cmd.getTenantId();
        RoleType roleType = cmd.getRoleType();
        String name = cmd.getName();
        String description = cmd.getDescription();

        Role role = new Role(roleType, name, description);
        role.setTenantId(tenantId);

        role = repository.save(role);

        AuditLogContext.current().addAuditObject(
                new AuditObject(role.getId().toString(), role.getName())
        );

        return new CreateRoleResponse(role.getId());
    }

    @Override
    @Transactional
    @ValidateRequest
    public UpdateRoleResponse updateRole(UpdateRoleCmd cmd) {
        Long roleId = cmd.getRoleId();
        RoleType roleType = cmd.getRoleType();
        String name = cmd.getName();
        String description = cmd.getDescription();

        Role role = repository.findRole(roleId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(role.getId().toString(), role.getName())
        );

        role.update(roleType, name, description);

        repository.save(role);

        return new UpdateRoleResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public DeleteRolesResponse deleteRoles(DeleteRolesCmd cmd) {
        List<Long> roleIds = cmd.getRoleIds();

        for (Long roleId : roleIds) {
            deleteRole(roleId);
        }

        return new DeleteRolesResponse();
    }

    private void deleteRole(Long roleId) {
        Role role = repository.findRole(roleId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(role.getId().toString(), role.getName())
        );

        repository.delete(role);
    }

    @Override
    @Transactional
    @ValidateRequest
    public AddPermissionsToRoleResponse addPermissionsToRole(AddPermissionsToRoleCmd cmd) {
        Long roleId = cmd.getRoleId();
        List<Long> permissionIds = cmd.getPermissionIds();

        Role role = repository.findRole(roleId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(role.getId().toString(), role.getName())
        );

        List<Permission> permissions = permissionRepository.findAllById(permissionIds);

        role.addPermissions(permissions);

        repository.save(role);

        return new AddPermissionsToRoleResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public RemovePermissionsFromRoleResponse removePermissionsFromRole(RemovePermissionsFromRoleCmd cmd) {
        Long roleId = cmd.getRoleId();
        List<Long> permissionIds = cmd.getPermissionIds();

        Role role = repository.findRole(roleId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(role.getId().toString(), role.getName())
        );

        role.removePermissions(permissionIds);

        repository.save(role);

        return new RemovePermissionsFromRoleResponse();
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedRoleResponse> describeRoles(DescribeRolesRequest request) {
        List<Long> roleIds = request.getRoleIds();
        List<RoleType> roleTypes = request.getRoleTypes();
        String search = request.getSearch();
        List<Long> userIds = request.getUserIds();
        Pageable pageable = request.getPageable();

        RoleFilters filters = new RoleFilters(roleIds, roleTypes, search, userIds);

        Page<Role> roles = repository.page(filters, pageable);

        return roles.map(assembler::toNestedRoleResponse);
    }


    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public DescribePermissionsResponse describePermissions(DescribePermissionsRequest request) {
        List<Permission> permissions = permissionRepository.findAll();
        List<NestedPermission> nestedPermissions = permissions.stream().map(assembler::toNestedPermission).toList();
        return new DescribePermissionsResponse(nestedPermissions);
    }
}
