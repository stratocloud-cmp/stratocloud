package com.stratocloud.group.jobs;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.group.UserGroupService;
import com.stratocloud.group.cmd.CreateUserGroupCmd;
import com.stratocloud.job.AutoRegisteredJobHandler;
import com.stratocloud.messaging.MessageBus;
import com.stratocloud.permission.DynamicPermissionRequired;
import com.stratocloud.permission.PermissionItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CreateUserGroupJobHandler
        implements AutoRegisteredJobHandler<CreateUserGroupCmd>, DynamicPermissionRequired {
    private final UserGroupService userGroupService;

    private final MessageBus messageBus;

    public CreateUserGroupJobHandler(UserGroupService userGroupService, MessageBus messageBus) {
        this.userGroupService = userGroupService;
        this.messageBus = messageBus;
    }

    @Override
    public String getJobType() {
        return "CREATE_USER_GROUP";
    }

    @Override
    public String getJobTypeName() {
        return "创建用户组";
    }

    @Override
    public String getStartJobTopic() {
        return "CREATE_USER_GROUP_JOB_START";
    }

    @Override
    public String getCancelJobTopic() {
        return "CREATE_USER_GROUP_JOB_CANCEL";
    }

    @Override
    public String getServiceName() {
        return StratoServices.IDENTITY_SERVICE;
    }

    @Override
    public void preCreateJob(CreateUserGroupCmd parameters) {

    }

    @Override
    public void onUpdateJob(CreateUserGroupCmd parameters) {

    }

    @Override
    public void onCancelJob(String message, CreateUserGroupCmd parameters) {

    }

    @Override
    public void onStartJob(CreateUserGroupCmd parameters) {
        tryFinishJob(messageBus, ()->userGroupService.createUserGroup(parameters));
    }

    @Override
    public List<String> collectSummaryData(CreateUserGroupCmd jobParameters) {
        return List.of("创建用户组: %s".formatted(jobParameters.getName()));
    }

    @Override
    public PermissionItem getPermissionItem() {
        return new PermissionItem("UserGroup", "用户组", "CREATE", "创建");
    }
}
