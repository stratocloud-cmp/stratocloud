package com.stratocloud.notification;

import com.stratocloud.notification.cmd.MarkInternalMailsReadCmd;
import com.stratocloud.notification.query.DescribeInternalMailRequest;
import com.stratocloud.notification.query.NestedInternalMail;
import com.stratocloud.notification.response.MarkInternalMailsReadResponse;
import org.springframework.data.domain.Page;

public interface InternalMailService {
    Page<NestedInternalMail> describeInternalMails(DescribeInternalMailRequest request);

    MarkInternalMailsReadResponse markInternalMailsRead(MarkInternalMailsReadCmd cmd);
}
