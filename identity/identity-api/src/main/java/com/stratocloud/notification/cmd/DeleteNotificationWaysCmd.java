package com.stratocloud.notification.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class DeleteNotificationWaysCmd implements ApiCommand {
    private List<Long> notificationWayIds;
    private boolean force;
}
