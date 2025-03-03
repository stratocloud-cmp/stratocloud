package com.stratocloud.group.jobs;

import com.stratocloud.auth.CallContext;
import com.stratocloud.constant.StratoServices;
import com.stratocloud.group.UserGroup;
import com.stratocloud.group.UserGroupService;
import com.stratocloud.group.cmd.AddUsersToGroupCmd;
import com.stratocloud.group.cmd.JoinUserGroupCmd;
import com.stratocloud.job.AutoRegisteredJobHandler;
import com.stratocloud.jpa.repository.EntityManager;
import com.stratocloud.messaging.MessageBus;
import com.stratocloud.permission.DynamicPermissionRequired;
import com.stratocloud.permission.PermissionItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InviteUsersToGroupJobHandler
        implements AutoRegisteredJobHandler<AddUsersToGroupCmd>, DynamicPermissionRequired {
    private final UserGroupService userGroupService;

    private final EntityManager entityManager;

    private final MessageBus messageBus;

    public InviteUsersToGroupJobHandler(UserGroupService userGroupService,
                                        EntityManager entityManager,
                                        MessageBus messageBus) {
        this.userGroupService = userGroupService;
        this.entityManager = entityManager;
        this.messageBus = messageBus;
    }

    @Override
    public String getJobType() {
        return "INVITE_USERS_TO_GROUP";
    }

    @Override
    public String getJobTypeName() {
        return "邀请加入用户组";
    }

    @Override
    public String getStartJobTopic() {
        return "INVITE_USERS_TO_GROUP_JOB_START";
    }

    @Override
    public String getCancelJobTopic() {
        return "INVITE_USERS_TO_GROUP_JOB_CANCEL";
    }

    @Override
    public String getServiceName() {
        return StratoServices.IDENTITY_SERVICE;
    }

    @Override
    public void preCreateJob(AddUsersToGroupCmd parameters) {

    }

    @Override
    public void onUpdateJob(AddUsersToGroupCmd parameters) {

    }

    @Override
    public void onCancelJob(String message, AddUsersToGroupCmd parameters) {

    }

    @Override
    public void onStartJob(AddUsersToGroupCmd parameters) {
        CallContext currentContext = CallContext.current();
        CallContext.registerSystemSession();
        tryFinishJob(messageBus, ()->userGroupService.addUsersToGroup(parameters));
        CallContext.registerBack(currentContext);
    }

    @Override
    public List<String> collectSummaryData(AddUsersToGroupCmd jobParameters) {
        UserGroup group = entityManager.findById(UserGroup.class, jobParameters.getUserGroupId());
        return List.of("邀请加入用户组: %s".formatted(group.getName()));
    }

    @Override
    public PermissionItem getPermissionItem() {
        return new PermissionItem("UserGroup", "用户组", "INVITE", "邀请用户");
    }
}
