package com.stratocloud.group.jobs;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.group.UserGroup;
import com.stratocloud.group.UserGroupService;
import com.stratocloud.group.cmd.RemoveUsersFromGroupCmd;
import com.stratocloud.job.AutoRegisteredJobHandler;
import com.stratocloud.jpa.repository.EntityManager;
import com.stratocloud.messaging.MessageBus;
import com.stratocloud.permission.DynamicPermissionRequired;
import com.stratocloud.permission.PermissionItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RemoveUsersFromGroupJobHandler
        implements AutoRegisteredJobHandler<RemoveUsersFromGroupCmd>, DynamicPermissionRequired {
    private final UserGroupService userGroupService;

    private final EntityManager entityManager;

    private final MessageBus messageBus;

    public RemoveUsersFromGroupJobHandler(UserGroupService userGroupService,
                                          EntityManager entityManager,
                                          MessageBus messageBus) {
        this.userGroupService = userGroupService;
        this.entityManager = entityManager;
        this.messageBus = messageBus;
    }

    @Override
    public String getJobType() {
        return "REMOVE_USERS_FROM_GROUP";
    }

    @Override
    public String getJobTypeName() {
        return "用户组移除成员";
    }

    @Override
    public String getStartJobTopic() {
        return "REMOVE_USERS_FROM_GROUP_JOB_START";
    }

    @Override
    public String getCancelJobTopic() {
        return "REMOVE_USERS_FROM_GROUP_JOB_CANCEL";
    }

    @Override
    public String getServiceName() {
        return StratoServices.IDENTITY_SERVICE;
    }

    @Override
    public void preCreateJob(RemoveUsersFromGroupCmd parameters) {
        validatePermission();
    }

    @Override
    public void onUpdateJob(RemoveUsersFromGroupCmd parameters) {

    }

    @Override
    public void onCancelJob(String message, RemoveUsersFromGroupCmd parameters) {

    }

    @Override
    public void onStartJob(RemoveUsersFromGroupCmd parameters) {
        tryFinishJob(messageBus, ()->userGroupService.removeUsersFromGroup(parameters));
    }

    @Override
    public List<String> collectSummaryData(RemoveUsersFromGroupCmd jobParameters) {
        UserGroup group = entityManager.findById(UserGroup.class, jobParameters.getUserGroupId());
        return List.of("用户组移除成员: %s".formatted(group.getName()));
    }

    @Override
    public PermissionItem getPermissionItem() {
        return new PermissionItem("UserGroup", "用户组", "REMOVE_USERS", "移除成员");
    }
}
