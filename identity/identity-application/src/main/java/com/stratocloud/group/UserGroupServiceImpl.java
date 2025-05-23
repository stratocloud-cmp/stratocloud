package com.stratocloud.group;

import com.stratocloud.audit.AuditLogContext;
import com.stratocloud.audit.AuditObject;
import com.stratocloud.auth.CallContext;
import com.stratocloud.group.cmd.*;
import com.stratocloud.group.query.DescribeGroupsRequest;
import com.stratocloud.group.query.DescribeSimpleGroupsResponse;
import com.stratocloud.group.query.NestedUserGroupResponse;
import com.stratocloud.group.response.*;
import com.stratocloud.repository.UserGroupRepository;
import com.stratocloud.repository.UserRepository;
import com.stratocloud.user.User;
import com.stratocloud.utils.Utils;
import com.stratocloud.validate.ValidateRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserGroupServiceImpl implements UserGroupService {

    private final UserGroupRepository repository;

    private final UserRepository userRepository;

    private final UserGroupAssembler assembler;

    public UserGroupServiceImpl(UserGroupRepository repository,
                                UserRepository userRepository,
                                UserGroupAssembler assembler) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.assembler = assembler;
    }

    @Override
    @Transactional
    @ValidateRequest
    public CreateUserGroupResponse createUserGroup(CreateUserGroupCmd cmd) {
        String name = cmd.getName();
        String alias = cmd.getAlias();
        String description = cmd.getDescription();
        List<NestedUserGroupTag> tags = cmd.getTags();

        UserGroup userGroup = new UserGroup(name, alias, description);

        List<UserGroupTag> userGroupTags = toUserGroupTags(tags);

        userGroup.updateTags(userGroupTags);

        userGroup.setTenantId(cmd.getTenantId());


        CallContext callContext = CallContext.current();
        if(!callContext.isAdmin()){
            User callingUser = userRepository.findUser(callContext.getCallingUser().userId());
            userGroup.addMember(callingUser);
        }


        userGroup = repository.save(userGroup);

        AuditLogContext.current().addAuditObject(
                new AuditObject(userGroup.getId().toString(), userGroup.getName())
        );

        return new CreateUserGroupResponse(userGroup.getId());
    }

    private List<UserGroupTag> toUserGroupTags(List<NestedUserGroupTag> tags) {
        if(Utils.isEmpty(tags))
            return new ArrayList<>();

        return tags.stream().map(
                t->new UserGroupTag(t.getTagKey(),t.getTagKeyName(),t.getTagValue(),t.getTagValueName())
        ).toList();
    }

    @Override
    @Transactional
    @ValidateRequest
    public UpdateUserGroupResponse updateUserGroup(UpdateUserGroupCmd cmd) {
        Long userGroupId = cmd.getUserGroupId();
        String name = cmd.getName();
        String alias = cmd.getAlias();
        String description = cmd.getDescription();
        List<NestedUserGroupTag> tags = cmd.getTags();

        UserGroup userGroup = repository.findUserGroup(userGroupId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(userGroup.getId().toString(), userGroup.getName())
        );

        userGroup.update(name, alias, description);
        userGroup.updateTags(toUserGroupTags(tags));

        return new UpdateUserGroupResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public DeleteUserGroupsResponse deleteUserGroups(DeleteUserGroupsCmd cmd) {
        List<Long> userGroupIds = cmd.getUserGroupIds();
        userGroupIds.forEach(this::deleteUserGroup);
        return new DeleteUserGroupsResponse();
    }

    private void deleteUserGroup(Long userGroupId) {
        UserGroup userGroup = repository.findUserGroup(userGroupId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(userGroup.getId().toString(), userGroup.getName())
        );

        repository.delete(userGroup);
    }

    @Override
    @Transactional
    @ValidateRequest
    public AddUsersToGroupResponse addUsersToGroup(AddUsersToGroupCmd cmd) {
        List<Long> userIds = cmd.getUserIds();
        Long userGroupId = cmd.getUserGroupId();

        UserGroup userGroup = repository.findUserGroup(userGroupId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(userGroup.getId().toString(), userGroup.getName())
        );

        for (Long userId : userIds) {
            User user = userRepository.findUser(userId);
            userGroup.addMember(user);
        }

        repository.save(userGroup);

        return new AddUsersToGroupResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public RemoveUserFromGroupResponse removeUsersFromGroup(RemoveUsersFromGroupCmd cmd) {
        Long userGroupId = cmd.getUserGroupId();
        List<Long> userIds = cmd.getUserIds();

        UserGroup userGroup = repository.findUserGroup(userGroupId);

        Long userId = CallContext.current().getCallingUser().userId();
        boolean isMemberCalling = userGroup.hasMember(userId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(userGroup.getId().toString(), userGroup.getName())
        );

        userIds.forEach(userGroup::removeMemberById);

        if(isMemberCalling){
            repository.saveWithSystemSession(userGroup);
        } else {
            repository.save(userGroup);
        }


        return new RemoveUserFromGroupResponse();
    }


    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public DescribeSimpleGroupsResponse describeSimpleUserGroups(DescribeGroupsRequest request) {
        List<Long> userGroupIds = request.getUserGroupIds();
        List<Long> userIds = request.getUserIds();
        String search = request.getSearch();
        List<NestedUserGroupTag> tags = request.getTags();

        List<UserGroup> userGroups = repository.findByFilters(
                userGroupIds, userIds, search, toTagsMap(tags), true
        );

        return assembler.toSimpleGroupsResponse(userGroups);
    }

    private Map<String, List<String>> toTagsMap(List<NestedUserGroupTag> tags) {
        Map<String, List<String>> map = new HashMap<>();

        if(Utils.isEmpty(tags))
            return map;

        for (NestedUserGroupTag tag : tags) {
            map.computeIfAbsent(tag.getTagKey(), k -> new ArrayList<>()).add(tag.getTagValue());
        }

        return map;
    }

    @Override
    @ValidateRequest
    public Page<NestedUserGroupResponse> describeUserGroups(DescribeGroupsRequest request) {
        List<Long> userGroupIds = request.getUserGroupIds();
        List<Long> userIds = request.getUserIds();
        String search = request.getSearch();
        Boolean allGroups = request.getAllGroups();

        List<NestedUserGroupTag> tags = request.getTags();

        Page<UserGroup> page = repository.page(
                userGroupIds, userIds, search, toTagsMap(tags), allGroups, request.getPageable()
        );
        return page.map(assembler::toNestedUserGroupResponse);
    }
}
