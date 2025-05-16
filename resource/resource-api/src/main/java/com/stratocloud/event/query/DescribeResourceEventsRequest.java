package com.stratocloud.event.query;

import com.stratocloud.event.StratoEventLevel;
import com.stratocloud.event.StratoEventSource;
import com.stratocloud.request.query.PagingRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DescribeResourceEventsRequest extends PagingRequest {
    private String search;

    private List<Long> resourceIds;
    private List<String> eventTypes;
    private List<StratoEventLevel> eventLevels;
    private List<StratoEventSource> eventSources;
}
