package com.stratocloud.tenant;

import com.stratocloud.audit.AuditLogContext;
import com.stratocloud.audit.AuditObject;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.InvalidArgumentException;
import com.stratocloud.identity.BuiltInIds;
import com.stratocloud.repository.RoleRepository;
import com.stratocloud.repository.TenantRepository;
import com.stratocloud.repository.UserGroupRepository;
import com.stratocloud.repository.UserRepository;
import com.stratocloud.tenant.cmd.*;
import com.stratocloud.tenant.query.*;
import com.stratocloud.tenant.response.*;
import com.stratocloud.utils.Utils;
import com.stratocloud.validate.ValidateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TenantServiceImpl implements TenantService {

    private final TenantRepository repository;

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final UserGroupRepository userGroupRepository;

    private final TenantAssembler assembler;

    public TenantServiceImpl(TenantRepository repository,
                             UserRepository userRepository,
                             RoleRepository roleRepository,
                             UserGroupRepository userGroupRepository, TenantAssembler assembler) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userGroupRepository = userGroupRepository;
        this.assembler = assembler;
    }

    @Override
    @Transactional
    @ValidateRequest
    public CreateTenantResponse createTenant(CreateTenantCmd cmd) {
        String name = cmd.getName();
        String description = cmd.getDescription();
        Long parentId = cmd.getParentId();

        if(repository.existsByName(name))
            throw new InvalidArgumentException("已存在租户: %s".formatted(name));

        Tenant tenant;

        if(parentId != null){
            Tenant parent = repository.findTenant(parentId);
            tenant = new Tenant(name, description, parent);
        }else {
            tenant = new Tenant(name, description);
        }

        tenant = repository.save(tenant);

        AuditLogContext.current().addAuditObject(
                new AuditObject(tenant.getId().toString(), tenant.getName())
        );

        return new CreateTenantResponse(tenant.getId());
    }

    @Override
    @Transactional
    @ValidateRequest
    public UpdateTenantResponse updateTenant(UpdateTenantCmd cmd) {
        Long tenantId = cmd.getTenantId();
        String name = cmd.getName();
        String description = cmd.getDescription();

        if(repository.existsByName(name))
            throw new InvalidArgumentException("已存在租户: %s".formatted(name));

        Tenant tenant = repository.findTenant(tenantId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(tenant.getId().toString(), tenant.getName())
        );

        tenant.update(name, description);

        repository.save(tenant);

        return new UpdateTenantResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public DisableTenantsResponse disableTenants(DisableTenantsCmd cmd) {
        List<Long> tenantIds = cmd.getTenantIds();

        for (Long tenantId : tenantIds) {
            disableTenant(tenantId);
        }

        return new DisableTenantsResponse();
    }

    private void disableTenant(Long tenantId) {
        Tenant tenant = repository.findTenant(tenantId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(tenant.getId().toString(), tenant.getName())
        );

        tenant.disable();
        repository.save(tenant);
    }

    @Override
    @Transactional
    @ValidateRequest
    public EnableTenantsResponse enableTenants(EnableTenantsCmd cmd) {
        List<Long> tenantIds = cmd.getTenantIds();

        for (Long tenantId : tenantIds) {
            enableTenant(tenantId);
        }

        return new EnableTenantsResponse();
    }

    private void enableTenant(Long tenantId) {
        Tenant tenant = repository.findTenant(tenantId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(tenant.getId().toString(), tenant.getName())
        );

        tenant.enable();
        repository.save(tenant);
    }

    @Override
    @Transactional
    @ValidateRequest
    public DeleteTenantsResponse deleteTenants(DeleteTenantsCmd cmd) {
        List<Long> tenantIds = cmd.getTenantIds();

        if(tenantIds.contains(BuiltInIds.ROOT_TENANT_ID))
            throw new BadCommandException("禁止删除根租户");

        if(userRepository.existsByTenantIds(tenantIds))
            throw new BadCommandException("租户下仍存在用户");

        if(roleRepository.existsByTenantIds(tenantIds))
            throw new BadCommandException("租户下仍存在角色");

        if(userGroupRepository.existsByTenantIds(tenantIds))
            throw new BadCommandException("租户下仍存在用户组");

        List<Tenant> tenants = repository.findAllById(tenantIds);

        for (Tenant tenant : tenants) {
            AuditLogContext.current().addAuditObject(
                    new AuditObject(tenant.getId().toString(), tenant.getName())
            );

            if(Utils.isNotEmpty(tenant.getChildren()))
                throw new BadCommandException("租户下仍存在子租户");
            tenant.onDelete();
            repository.delete(tenant);
        }

        return new DeleteTenantsResponse();
    }


    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public DescribeSimpleTenantsResponse describeInheritedTenants(DescribeInheritedTenantsRequest request) {
        Long tenantId = request.getTenantId();

        List<Tenant> inheritedTenants = repository.findInheritedTenants(tenantId);

        return assembler.toSimpleTenantsResponse(inheritedTenants);
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public DescribeSimpleTenantsResponse describeSubTenants(DescribeSubTenantsRequest request) {
        Long tenantId = request.getTenantId();

        List<Tenant> subTenants = repository.findSubTenants(tenantId);

        return assembler.toSimpleTenantsResponse(subTenants);
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedTenantResponse> describeTenants(DescribeTenantsRequest request) {
        List<Long> tenantIds = request.getTenantIds();
        String search = request.getSearch();
        Boolean disabled = request.getDisabled();
        List<Long> parentIds = request.getParentIds();
        Boolean includeInherited = request.getIncludeInherited();
        Pageable pageable = request.getPageable();

        TenantFilters filters = new TenantFilters(tenantIds, search, disabled, parentIds, includeInherited);

        Page<Tenant> page = repository.page(filters, pageable);

        return page.map(assembler::toNestedTenantResponse);
    }


    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public DescribeTenantsTreeResponse describeTenantsTree(DescribeTenantsTreeRequest request) {
        List<Tenant> roots = repository.findVisibleRoots(request.getIncludeInherited());

        return assembler.toTenantsTreeResponse(roots);
    }
}
