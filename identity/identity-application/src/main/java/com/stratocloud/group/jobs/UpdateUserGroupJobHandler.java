package com.stratocloud.group.jobs;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.group.UserGroup;
import com.stratocloud.group.UserGroupService;
import com.stratocloud.group.cmd.UpdateUserGroupCmd;
import com.stratocloud.job.AutoRegisteredJobHandler;
import com.stratocloud.job.JobContext;
import com.stratocloud.jpa.repository.EntityManager;
import com.stratocloud.messaging.MessageBus;
import com.stratocloud.permission.DynamicPermissionRequired;
import com.stratocloud.permission.PermissionItem;
import com.stratocloud.tag.NestedTag;
import com.stratocloud.tag.TagRecord;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class UpdateUserGroupJobHandler
        implements AutoRegisteredJobHandler<UpdateUserGroupCmd>, DynamicPermissionRequired {
    private final UserGroupService userGroupService;

    private final EntityManager entityManager;

    private final MessageBus messageBus;

    public UpdateUserGroupJobHandler(UserGroupService userGroupService,
                                     EntityManager entityManager,
                                     MessageBus messageBus) {
        this.userGroupService = userGroupService;
        this.entityManager = entityManager;
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
        validatePermission();
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

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> prepareRuntimeProperties(UpdateUserGroupCmd jobParameters) {
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
