package com.stratocloud.notification;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.notification.cmd.MarkInternalMailsReadCmd;
import com.stratocloud.notification.query.DescribeInternalMailRequest;
import com.stratocloud.notification.query.NestedInternalMail;
import com.stratocloud.notification.response.MarkInternalMailsReadResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface InternalMailApi {

    @PostMapping(StratoServices.IDENTITY_SERVICE+"/describe-internal-mails")
    Page<NestedInternalMail> describeInternalMails(@RequestBody DescribeInternalMailRequest request);

    @PostMapping(StratoServices.IDENTITY_SERVICE+"/mark-internal-mails-read")
    MarkInternalMailsReadResponse markInternalMailsRead(@RequestBody MarkInternalMailsReadCmd cmd);
}
