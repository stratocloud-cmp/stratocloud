package com.stratocloud.controllers;

import com.stratocloud.notification.InternalMailApi;
import com.stratocloud.notification.InternalMailService;
import com.stratocloud.notification.cmd.MarkInternalMailsReadCmd;
import com.stratocloud.notification.query.DescribeInternalMailsRequest;
import com.stratocloud.notification.query.NestedInternalMail;
import com.stratocloud.notification.response.MarkInternalMailsReadResponse;
import com.stratocloud.permission.PermissionTarget;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PermissionTarget(target = "InternalMail", targetName = "站内信")
public class InternalMailController implements InternalMailApi {

    private final InternalMailService service;

    public InternalMailController(InternalMailService service) {
        this.service = service;
    }

    @Override
    public Page<NestedInternalMail> describeInternalMails(@RequestBody DescribeInternalMailsRequest request) {
        return service.describeInternalMails(request);
    }

    @Override
    public MarkInternalMailsReadResponse markInternalMailsRead(@RequestBody MarkInternalMailsReadCmd cmd) {
        return service.markInternalMailsRead(cmd);
    }
}
