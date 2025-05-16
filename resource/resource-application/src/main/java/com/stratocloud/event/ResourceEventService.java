package com.stratocloud.event;

import com.stratocloud.event.query.DescribeResourceEventsRequest;
import com.stratocloud.event.response.NestedResourceEvent;
import org.springframework.data.domain.Page;

public interface ResourceEventService {
    Page<NestedResourceEvent> describeResourceEvents(DescribeResourceEventsRequest request);
}
