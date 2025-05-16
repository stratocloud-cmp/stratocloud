package com.stratocloud.repository;

import com.stratocloud.event.StratoEventLevel;
import com.stratocloud.event.StratoEventSource;
import com.stratocloud.jpa.repository.ControllableRepository;
import com.stratocloud.resource.event.ResourceEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ResourceEventRepository extends ControllableRepository<ResourceEvent> {
    Page<ResourceEvent> page(String search,
                             List<Long> resourceIds,
                             List<String> eventTypes,
                             List<StratoEventLevel> eventLevels,
                             List<StratoEventSource> eventSources,
                             Pageable pageable);

    List<ResourceEvent> findAllByResourceId(Long resourceId);
}
