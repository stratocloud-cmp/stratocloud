package com.stratocloud.event;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.event.query.DescribeResourceEventsRequest;
import com.stratocloud.event.response.NestedResourceEvent;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface ResourceEventApi {
    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-resource-events")
    Page<NestedResourceEvent> describeResourceEvents(@RequestBody DescribeResourceEventsRequest request);
}
