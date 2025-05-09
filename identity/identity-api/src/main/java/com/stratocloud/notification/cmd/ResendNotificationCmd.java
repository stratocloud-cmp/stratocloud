package com.stratocloud.notification.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class ResendNotificationCmd implements ApiCommand {
    private Long notificationId;
    private List<Long> receiverUserIds;
}
