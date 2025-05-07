package com.stratocloud.notification;

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

public interface NotificationWayService {
    CreateNotificationWayResponse createNotificationWay(CreateNotificationWayCmd cmd);

    UpdateNotificationWayResponse updateNotificationWay(UpdateNotificationWayCmd cmd);

    DeleteNotificationWaysResponse deleteNotificationWays(DeleteNotificationWaysCmd cmd);

    Page<NestedNotificationWay> describeNotificationWays(DescribeNotificationWaysRequest request);

    DescribeNotificationProvidersResponse describeNotificationProviders(DescribeNotificationProvidersRequest request);
}
