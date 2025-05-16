package com.stratocloud.controllers;

import com.stratocloud.event.ResourceEventApi;
import com.stratocloud.event.ResourceEventService;
import com.stratocloud.event.query.DescribeResourceEventsRequest;
import com.stratocloud.event.response.NestedResourceEvent;
import com.stratocloud.permission.PermissionTarget;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PermissionTarget(target = "ResourceEvent", targetName = "云资源事件")
public class ResourceEventController implements ResourceEventApi {

    private final ResourceEventService service;

    public ResourceEventController(ResourceEventService service) {
        this.service = service;
    }

    @Override
    public Page<NestedResourceEvent> describeResourceEvents(@RequestBody DescribeResourceEventsRequest request) {
        return service.describeResourceEvents(request);
    }
}
