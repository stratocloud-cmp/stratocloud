package com.stratocloud.notification.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class DeleteNotificationPoliciesCmd implements ApiCommand {
    private List<Long> notificationPolicyIds;
    private boolean force;
}
