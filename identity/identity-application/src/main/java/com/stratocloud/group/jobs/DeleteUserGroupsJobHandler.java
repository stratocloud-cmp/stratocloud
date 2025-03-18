package com.stratocloud.group.jobs;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.group.UserGroupService;
import com.stratocloud.group.cmd.DeleteUserGroupsCmd;
import com.stratocloud.job.AutoRegisteredJobHandler;
import com.stratocloud.messaging.MessageBus;
import com.stratocloud.permission.DynamicPermissionRequired;
import com.stratocloud.permission.PermissionItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeleteUserGroupsJobHandler
        implements AutoRegisteredJobHandler<DeleteUserGroupsCmd>, DynamicPermissionRequired {
    private final UserGroupService userGroupService;

    private final MessageBus messageBus;

    public DeleteUserGroupsJobHandler(UserGroupService userGroupService, MessageBus messageBus) {
        this.userGroupService = userGroupService;
        this.messageBus = messageBus;
    }

    @Override
    public String getJobType() {
        return "DELETE_USER_GROUPS";
    }

    @Override
    public String getJobTypeName() {
        return "删除用户组";
    }

    @Override
    public String getStartJobTopic() {
        return "DELETE_USER_GROUPS_JOB_START";
    }

    @Override
    public String getCancelJobTopic() {
        return "DELETE_USER_GROUPS_JOB_CANCEL";
    }

    @Override
    public String getServiceName() {
        return StratoServices.IDENTITY_SERVICE;
    }

    @Override
    public void preCreateJob(DeleteUserGroupsCmd parameters) {
        validatePermission();
    }

    @Override
    public void onUpdateJob(DeleteUserGroupsCmd parameters) {

    }

    @Override
    public void onCancelJob(String message, DeleteUserGroupsCmd parameters) {

    }

    @Override
    public void onStartJob(DeleteUserGroupsCmd parameters) {
        tryFinishJob(messageBus, ()->userGroupService.deleteUserGroups(parameters));
    }

    @Override
    public List<String> collectSummaryData(DeleteUserGroupsCmd jobParameters) {
        return List.of("删除用户组: %s".formatted(jobParameters.getUserGroupIds()));
    }

    @Override
    public PermissionItem getPermissionItem() {
        return new PermissionItem("UserGroup", "用户组", "DELETE", "删除");
    }
}
