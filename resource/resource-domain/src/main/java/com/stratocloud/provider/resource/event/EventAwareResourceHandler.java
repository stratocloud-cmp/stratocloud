package com.stratocloud.provider.resource.event;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.event.ExternalResourceEvent;
import com.stratocloud.provider.resource.ResourceHandler;

import java.time.LocalDateTime;
import java.util.List;

public interface EventAwareResourceHandler extends ResourceHandler {
    List<ExternalResourceEvent> describeResourceEvents(ExternalAccount account,
                                                       String externalId,
                                                       LocalDateTime happenedAfter);
}
