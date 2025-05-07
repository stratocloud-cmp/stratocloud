package com.stratocloud.notification;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.notification.cmd.CreateNotificationWayCmd;
import com.stratocloud.notification.cmd.DeleteNotificationWaysCmd;
import com.stratocloud.notification.cmd.UpdateNotificationWayCmd;
import com.stratocloud.notification.query.DescribeNotificationProvidersRequest;
import com.stratocloud.notification.query.DescribeNotificationProvidersResponse;
import com.stratocloud.notification.query.DescribeNotificationWaysRequest;
import com.stratocloud.notification.query.NestedNotificationWay;
import com.stratocloud.notification.response.CreateNotificationWayResponse;
import com.stratocloud.notification.response.DeleteNotificationWaysResponse;
import com.stratocloud.notification.response.UpdateNotificationWayResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface NotificationWayApi {
    @PostMapping(StratoServices.IDENTITY_SERVICE+"/create-notification-way")
    CreateNotificationWayResponse createNotificationWay(@RequestBody CreateNotificationWayCmd cmd);

    @PostMapping(StratoServices.IDENTITY_SERVICE+"/update-notification-way")
    UpdateNotificationWayResponse updateNotificationWay(@RequestBody UpdateNotificationWayCmd cmd);

    @PostMapping(StratoServices.IDENTITY_SERVICE+"/delete-notification-ways")
    DeleteNotificationWaysResponse deleteNotificationWays(@RequestBody DeleteNotificationWaysCmd cmd);

    @PostMapping(StratoServices.IDENTITY_SERVICE+"/describe-notification-ways")
    Page<NestedNotificationWay> describeNotificationWays(@RequestBody DescribeNotificationWaysRequest request);

    @PostMapping(StratoServices.IDENTITY_SERVICE+"/describe-notification-providers")
    DescribeNotificationProvidersResponse describeNotificationProviders(DescribeNotificationProvidersRequest request);
}
