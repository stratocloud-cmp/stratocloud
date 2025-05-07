package com.stratocloud.notification;

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

public interface NotificationPolicyService {
    CreateNotificationPolicyResponse createNotificationPolicy(CreateNotificationPolicyCmd cmd);

    UpdateNotificationPolicyResponse updateNotificationPolicy(UpdateNotificationPolicyCmd cmd);

    DeleteNotificationPoliciesResponse deleteNotificationPolicies(DeleteNotificationPoliciesCmd cmd);

    Page<NestedNotificationPolicy> describeNotificationPolicies(DescribeNotificationPoliciesRequest request);

    DescribeNotificationEventTypesResponse describeNotificationEventTypes(DescribeNotificationEventTypesRequest request);
}
