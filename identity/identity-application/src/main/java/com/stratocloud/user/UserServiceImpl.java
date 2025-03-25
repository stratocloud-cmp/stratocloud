package com.stratocloud.user;

import com.stratocloud.audit.AuditLogContext;
import com.stratocloud.audit.AuditObject;
import com.stratocloud.auth.CallContext;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.group.UserGroup;
import com.stratocloud.identity.BuiltInIds;
import com.stratocloud.repository.RoleRepository;
import com.stratocloud.repository.UserGroupRepository;
import com.stratocloud.repository.UserRepository;
import com.stratocloud.role.Role;
import com.stratocloud.user.cmd.*;
import com.stratocloud.user.query.DescribeUsersRequest;
import com.stratocloud.user.query.DescribeUsersSimpleResponse;
import com.stratocloud.user.query.UserResponse;
import com.stratocloud.user.response.*;
import com.stratocloud.validate.ValidateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class UserServiceImpl implements UserService{

    private final UserRepository repository;

    private final UserAssembler assembler;

    private final UserFactory userFactory;

    private final RoleRepository roleRepository;

    private final UserGroupRepository userGroupRepository;


    public UserServiceImpl(UserRepository repository,
                           UserAssembler assembler,
                           UserFactory userFactory,
                           RoleRepository roleRepository,
                           UserGroupRepository userGroupRepository) {
        this.repository = repository;
        this.assembler = assembler;
        this.userFactory = userFactory;
        this.roleRepository = roleRepository;
        this.userGroupRepository = userGroupRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public DescribeUsersSimpleResponse describeSimpleUsers(DescribeUsersRequest request) {
        UserFilters userFilters = getUserFilters(request);

        List<User> users = repository.findAllByFilters(userFilters);

        return assembler.toSimpleUsersResponse(users);
    }

    private static UserFilters getUserFilters(DescribeUsersRequest request) {
        List<Long> tenantIds = request.getTenantIds();
        List<Long> userIds = request.getUserIds();
        List<Long> roleIds = request.getRoleIds();
        List<Long> userGroupIds = request.getUserGroupIds();
        String search = request.getSearch();
        Boolean disabled = request.getDisabled();
        Boolean locked = request.getLocked();
        return new UserFilters(tenantIds, userIds, roleIds, userGroupIds, search, disabled, locked);
    }

    @Override
    @Transactional
    @ValidateRequest
    public CreateUserResponse createUser(CreateUserCmd cmd) {
        User user = userFactory.createUser(cmd);

        user = repository.save(user);

        AuditLogContext.current().addAuditObject(
                new AuditObject(user.getId().toString(), user.getRealName())
        );

        return new CreateUserResponse(user.getId());
    }


    @Override
    @Transactional
    @ValidateRequest
    public UpdateUserResponse updateUser(UpdateUserCmd cmd) {
        Long userId = cmd.getUserId();
        String realName = cmd.getRealName();
        String emailAddress = cmd.getEmailAddress();
        String phoneNumber = cmd.getPhoneNumber();
        String description = cmd.getDescription();

        boolean updatingSelf = userId.equals(CallContext.current().getCallingUser().userId());
        if(!updatingSelf)
            CallContext.current().validatePermission("User", "UPDATE");

        User user = repository.findUser(userId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(user.getId().toString(), user.getRealName())
        );

        user.update(realName, emailAddress, phoneNumber, description);

        if(updatingSelf)
            repository.saveWithSystemSession(user);
        else
            repository.save(user);

        return new UpdateUserResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public DisableUsersResponse disableUsers(DisableUsersCmd cmd) {
        for (Long userId : cmd.getUserIds())
            disableUser(userId);

        return new DisableUsersResponse();
    }

    private void disableUser(Long userId) {
        User user = repository.findUser(userId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(user.getId().toString(), user.getRealName())
        );

        user.disable();
        repository.save(user);
    }

    @Override
    @Transactional
    @ValidateRequest
    public EnableUsersResponse enableUsers(EnableUsersCmd cmd) {
        for (Long userId : cmd.getUserIds()) {
            enableUser(userId);
        }
        return new EnableUsersResponse();
    }

    private void enableUser(Long userId) {
        User user = repository.findUser(userId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(user.getId().toString(), user.getRealName())
        );

        user.enable();
        repository.save(user);
    }

    @Override
    @Transactional
    @ValidateRequest
    public UnlockUsersResponse unlockUsers(UnlockUsersCmd cmd) {
        for (Long userId : cmd.getUserIds()) {
            unlockUser(userId);
        }
        return new UnlockUsersResponse();
    }

    private void unlockUser(Long userId) {
        User user = repository.findUser(userId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(user.getId().toString(), user.getRealName())
        );

        user.unlock();
        repository.save(user);
    }


    @Override
    @Transactional
    @ValidateRequest
    public DeleteUsersResponse deleteUsers(DeleteUsersCmd cmd) {
        for (Long userId : cmd.getUserIds()) {
            deleteUser(userId);
        }
        return new DeleteUsersResponse();
    }

    private void deleteUser(Long userId) {
        User user = repository.findUser(userId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(user.getId().toString(), user.getRealName())
        );

        Set<Long> builtInUserIds = Set.of(BuiltInIds.SYSTEM_USER_ID, BuiltInIds.SUPER_ADMIN_USER_ID);

        if(builtInUserIds.contains(userId))
            throw new BadCommandException("内置用户无法删除");

        List<UserGroup> userGroups = userGroupRepository.findByFilters(null, List.of(userId), null);
        if(!userGroups.isEmpty())
            throw new BadCommandException("请先在相关用户组中移除需要删除的用户");

        user.onDelete();
        repository.delete(user);
    }

    @Override
    @Transactional
    @ValidateRequest
    public BatchAssignRoleToUserResponse batchAssignRoleToUser(BatchAssignRoleToUserCmd cmd) {
        for (NestedAssignRoleToUserCmd roleToUserCmd : cmd.getAssignList()) {
            assignRoleToUser(roleToUserCmd);
        }
        return new BatchAssignRoleToUserResponse();
    }

    private void assignRoleToUser(NestedAssignRoleToUserCmd roleToUserCmd) {
        Long roleId = roleToUserCmd.getRoleId();
        Long userId = roleToUserCmd.getUserId();
        List<Long> grantedTenantIds = roleToUserCmd.getGrantedTenantIds();

        Role role = roleRepository.findRole(roleId);
        User user = repository.findUser(userId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(user.getId().toString(), user.getRealName())
        );

        user.assignRole(role, grantedTenantIds);

        repository.save(user);
    }

    @Override
    @Transactional
    @ValidateRequest
    public BatchRemoveRoleFromUserResponse batchRemoveRoleFromUser(BatchRemoveRoleFromUserCmd cmd) {
        for (NestedRemoveRoleFromUserCmd removeRoleFromUserCmd : cmd.getRemoveList()) {
            removeRoleFromUser(removeRoleFromUserCmd);
        }
        return new BatchRemoveRoleFromUserResponse();
    }

    private void removeRoleFromUser(NestedRemoveRoleFromUserCmd removeRoleFromUserCmd) {
        Long roleId = removeRoleFromUserCmd.getRoleId();
        Long userId = removeRoleFromUserCmd.getUserId();

        User user = repository.findUser(userId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(user.getId().toString(), user.getRealName())
        );

        user.removeRoleById(roleId);

        repository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<UserResponse> describeUsers(DescribeUsersRequest request){
        UserFilters userFilters = getUserFilters(request);
        Page<User> page = repository.page(userFilters, request.getPageable());
        return page.map(UserAssembler::toUserResponse);
    }

    @Override
    @Transactional
    @ValidateRequest
    public ChangePasswordResponse changePassword(ChangePasswordCmd cmd) {
        boolean changingSelf = cmd.getUserId().equals(CallContext.current().getCallingUser().userId());

        if(!changingSelf)
            CallContext.current().validatePermission("User", "UPDATE");

        User user = repository.findUser(cmd.getUserId());

        AuditLogContext.current().addAuditObject(
                new AuditObject(user.getId().toString(), user.getRealName())
        );

        EncodedPassword encodedPassword = UserFactory.getEncodedPassword(cmd.getNewPassword(), user.getAuthType());

        user.changePassword(encodedPassword);

        if(changingSelf)
            repository.saveWithSystemSession(user);
        else
            repository.save(user);

        return new ChangePasswordResponse();
    }
}
