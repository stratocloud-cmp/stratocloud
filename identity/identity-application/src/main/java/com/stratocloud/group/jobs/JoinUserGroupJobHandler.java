package com.stratocloud.group.jobs;

import com.stratocloud.auth.CallContext;
import com.stratocloud.constant.StratoServices;
import com.stratocloud.group.UserGroup;
import com.stratocloud.group.UserGroupService;
import com.stratocloud.group.cmd.AddUsersToGroupCmd;
import com.stratocloud.group.cmd.JoinUserGroupCmd;
import com.stratocloud.job.AutoRegisteredJobHandler;
import com.stratocloud.job.JobContext;
import com.stratocloud.jpa.repository.EntityManager;
import com.stratocloud.messaging.MessageBus;
import com.stratocloud.permission.DynamicPermissionRequired;
import com.stratocloud.permission.PermissionItem;
import com.stratocloud.tag.NestedTag;
import com.stratocloud.tag.TagRecord;
import com.stratocloud.utils.Utils;
import org.apache.commons.collections.MapUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        validatePermission();
        JobContext.current().addOutput("newMemberId", CallContext.current().getCallingUser().userId());
    }

    @Override
    public void onUpdateJob(JoinUserGroupCmd parameters) {

    }

    @Override
    public void onCancelJob(String message, JoinUserGroupCmd parameters) {

    }

    @Override
    public void onStartJob(JoinUserGroupCmd parameters) {
        Long newMemberId = MapUtils.getLong(JobContext.current().getRuntimeVariables(), "newMemberId");

        AddUsersToGroupCmd cmd = new AddUsersToGroupCmd();
        cmd.setUserGroupId(parameters.getUserGroupId());
        CallContext currentContext = CallContext.current();
        cmd.setUserIds(List.of(newMemberId));
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

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> prepareRuntimeProperties(JoinUserGroupCmd jobParameters) {
        List<NestedTag> nestedTags = new ArrayList<>();

        if(jobParameters.getUserGroupId() != null){
            UserGroup group = entityManager.findById(UserGroup.class, jobParameters.getUserGroupId());

            if(Utils.isNotEmpty(group.getTags()))
                nestedTags.addAll(group.getTags());
        }

        return Map.of(
                JobContext.KEY_RELATED_TAGS,
                TagRecord.fromNestedTags(nestedTags)
        );
    }
}
