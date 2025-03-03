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
public class JoinUserGroupJobHandler
        implements AutoRegisteredJobHandler<JoinUserGroupCmd>, DynamicPermissionRequired {
    private final UserGroupService userGroupService;

    private final EntityManager entityManager;

    private final MessageBus messageBus;

    public JoinUserGroupJobHandler(UserGroupService userGroupService,
                                   EntityManager entityManager,
                                   MessageBus messageBus) {
        this.userGroupService = userGroupService;
        this.entityManager = entityManager;
        this.messageBus = messageBus;
    }

    @Override
    public String getJobType() {
        return "JOIN_USER_GROUP";
    }

    @Override
    public String getJobTypeName() {
        return "申请加入用户组";
    }

    @Override
    public String getStartJobTopic() {
        return "JOIN_USER_GROUP_JOB_START";
    }

    @Override
    public String getCancelJobTopic() {
        return "JOIN_USER_GROUP_JOB_CANCEL";
    }

    @Override
    public String getServiceName() {
        return StratoServices.IDENTITY_SERVICE;
    }

    @Override
    public void preCreateJob(JoinUserGroupCmd parameters) {

    }

    @Override
    public void onUpdateJob(JoinUserGroupCmd parameters) {

    }

    @Override
    public void onCancelJob(String message, JoinUserGroupCmd parameters) {

    }

    @Override
    public void onStartJob(JoinUserGroupCmd parameters) {
        AddUsersToGroupCmd cmd = new AddUsersToGroupCmd();
        cmd.setUserGroupId(parameters.getUserGroupId());
        CallContext currentContext = CallContext.current();
        cmd.setUserIds(List.of(currentContext.getCallingUser().userId()));
        CallContext.registerSystemSession();
        tryFinishJob(messageBus, ()->userGroupService.addUsersToGroup(cmd));
        CallContext.registerBack(currentContext);
    }

    @Override
    public List<String> collectSummaryData(JoinUserGroupCmd jobParameters) {
        UserGroup group = entityManager.findById(UserGroup.class, jobParameters.getUserGroupId());
        return List.of("申请加入用户组: %s".formatted(group.getName()));
    }

    @Override
    public PermissionItem getPermissionItem() {
        return new PermissionItem("UserGroup", "用户组", "JOIN", "申请加入");
    }
}
