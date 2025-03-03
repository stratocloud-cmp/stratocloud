package com.stratocloud.group.jobs;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.group.UserGroupService;
import com.stratocloud.group.cmd.UpdateUserGroupCmd;
import com.stratocloud.job.AutoRegisteredJobHandler;
import com.stratocloud.messaging.MessageBus;
import com.stratocloud.permission.DynamicPermissionRequired;
import com.stratocloud.permission.PermissionItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UpdateUserGroupJobHandler
        implements AutoRegisteredJobHandler<UpdateUserGroupCmd>, DynamicPermissionRequired {
    private final UserGroupService userGroupService;

    private final MessageBus messageBus;

    public UpdateUserGroupJobHandler(UserGroupService userGroupService, MessageBus messageBus) {
        this.userGroupService = userGroupService;
        this.messageBus = messageBus;
    }

    @Override
    public String getJobType() {
        return "UPDATE_USER_GROUP";
    }

    @Override
    public String getJobTypeName() {
        return "更新用户组";
    }

    @Override
    public String getStartJobTopic() {
        return "UPDATE_USER_GROUP_JOB_START";
    }

    @Override
    public String getCancelJobTopic() {
        return "UPDATE_USER_GROUP_JOB_CANCEL";
    }

    @Override
    public String getServiceName() {
        return StratoServices.IDENTITY_SERVICE;
    }

    @Override
    public void preCreateJob(UpdateUserGroupCmd parameters) {

    }

    @Override
    public void onUpdateJob(UpdateUserGroupCmd parameters) {

    }

    @Override
    public void onCancelJob(String message, UpdateUserGroupCmd parameters) {

    }

    @Override
    public void onStartJob(UpdateUserGroupCmd parameters) {
        tryFinishJob(messageBus, ()->userGroupService.updateUserGroup(parameters));
    }

    @Override
    public List<String> collectSummaryData(UpdateUserGroupCmd jobParameters) {
        return List.of("更新用户组: %s".formatted(jobParameters.getName()));
    }

    @Override
    public PermissionItem getPermissionItem() {
        return new PermissionItem("UserGroup", "用户组", "UPDATE", "更新");
    }
}
