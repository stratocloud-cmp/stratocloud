package com.stratocloud.provider.resource.monitor;

import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.monitor.ResourceQuickStats;

import java.util.Optional;

public interface MonitoredResourceHandler extends ResourceHandler {
    Optional<ResourceQuickStats> describeQuickStats(Resource resource);
}
