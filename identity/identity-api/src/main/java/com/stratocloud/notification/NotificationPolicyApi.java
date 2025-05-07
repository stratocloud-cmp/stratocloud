package com.stratocloud.notification;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.notification.cmd.CreateNotificationPolicyCmd;
import com.stratocloud.notification.cmd.DeleteNotificationPoliciesCmd;
import com.stratocloud.notification.cmd.UpdateNotificationPolicyCmd;
import com.stratocloud.notification.query.DescribeNotificationEventTypesRequest;
import com.stratocloud.notification.query.DescribeNotificationEventTypesResponse;
import com.stratocloud.notification.query.DescribeNotificationPoliciesRequest;
import com.stratocloud.notification.query.NestedNotificationPolicy;
import com.stratocloud.notification.response.CreateNotificationPolicyResponse;
import com.stratocloud.notification.response.DeleteNotificationPoliciesResponse;
import com.stratocloud.notification.response.UpdateNotificationPolicyResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface NotificationPolicyApi {

    @PostMapping(StratoServices.IDENTITY_SERVICE+"/create-notification-policy")
    CreateNotificationPolicyResponse createNotificationPolicy(@RequestBody CreateNotificationPolicyCmd cmd);

    @PostMapping(StratoServices.IDENTITY_SERVICE+"/update-notification-policy")
    UpdateNotificationPolicyResponse updateNotificationPolicy(@RequestBody UpdateNotificationPolicyCmd cmd);

    @PostMapping(StratoServices.IDENTITY_SERVICE+"/delete-notification-policies")
    DeleteNotificationPoliciesResponse deleteNotificationPolicies(@RequestBody DeleteNotificationPoliciesCmd cmd);

    @PostMapping(StratoServices.IDENTITY_SERVICE+"/describe-notification-policies")
    Page<NestedNotificationPolicy> describeNotificationPolicies(@RequestBody DescribeNotificationPoliciesRequest request);

    @PostMapping(StratoServices.IDENTITY_SERVICE+"/describe-notification-event-types")
    DescribeNotificationEventTypesResponse describeNotificationEventTypes(@RequestBody DescribeNotificationEventTypesRequest request);
}
