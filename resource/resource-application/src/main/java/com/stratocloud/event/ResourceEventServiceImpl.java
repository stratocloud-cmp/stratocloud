package com.stratocloud.event;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.auth.RunWithSystemSession;
import com.stratocloud.event.query.DescribeResourceEventsRequest;
import com.stratocloud.event.response.NestedResourceEvent;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.lock.DistributedLock;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.ProviderRegistry;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.event.EventAwareResourceHandler;
import com.stratocloud.repository.ResourceEventRepository;
import com.stratocloud.repository.ResourceRepository;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceFilters;
import com.stratocloud.resource.event.ResourceEvent;
import com.stratocloud.utils.Utils;
import com.stratocloud.validate.ValidateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ResourceEventServiceImpl implements ResourceEventService {

    private final ResourceEventRepository repository;

    private final ResourceRepository resourceRepository;

    public ResourceEventServiceImpl(ResourceEventRepository repository,
                                    ResourceRepository resourceRepository) {
        this.repository = repository;
        this.resourceRepository = resourceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedResourceEvent> describeResourceEvents(@RequestBody DescribeResourceEventsRequest request) {
        Page<ResourceEvent> page = repository.page(
                request.getSearch(),
                request.getResourceIds(),
                request.getEventTypes(),
                request.getEventLevels(),
                request.getEventSources(),
                request.getPageable()
        );

        return page.map(this::toNestedResourceEvent);
    }

    @Scheduled(fixedDelay = 30L, timeUnit = TimeUnit.SECONDS)
    @DistributedLock(lockName = "COLLECT_EXTERNAL_EVENTS", waitIfLocked = false)
    @RunWithSystemSession
    public void collectExternalEvents() {
        List<Provider> providers = ProviderRegistry.getProviders();

        for (Provider provider : providers) {
            List<? extends ResourceHandler> resourceHandlers = provider.getResourceHandlers();

            if(Utils.isEmpty(resourceHandlers))
                continue;

            for (ResourceHandler resourceHandler : resourceHandlers) {
                if(resourceHandler instanceof EventAwareResourceHandler)
                    collectExternalEventsByType(resourceHandler.getResourceTypeId());
            }
        }
    }

    public void collectExternalEventsByType(String resourceTypeId) {
        List<Long> resourceIds = resourceRepository.findAllByFilters(
                ResourceFilters.builder()
                        .resourceTypes(List.of(resourceTypeId))
                        .build()
        ).stream().map(Resource::getId).toList();

        if(Utils.isEmpty(resourceIds))
            return;

        ResourceEventServiceImpl currentProxy = (ResourceEventServiceImpl) AopContext.currentProxy();
        for (Long resourceId : resourceIds) {
            try {
                currentProxy.collectExternalEventsByResourceId(resourceId);
            }catch (Exception e){
                log.warn("Failed to collect external events for resource: {}.", resourceId, e);
            }
        }
    }

    @Transactional
    public void collectExternalEventsByResourceId(Long resourceId) {
        Resource resource = resourceRepository.findResource(resourceId);

        ResourceHandler resourceHandler = resource.getResourceHandler();

        if(!(resourceHandler instanceof EventAwareResourceHandler eventAwareResourceHandler))
            return;

        ExternalAccount account
                = resourceHandler.getAccountRepository().findExternalAccount(resource.getAccountId());
        List<ResourceEvent> events = repository.findAllByResourceId(resource.getId());

        LocalDateTime maxTime = events.stream().map(
                ResourceEvent::getEventHappenedAt
        ).max(
                Comparator.comparingLong(t -> t.atZone(ZoneId.systemDefault()).toEpochSecond())
        ).orElse(LocalDateTime.MIN);

        maxTime = LocalDateTime.of(maxTime.toLocalDate(), LocalTime.MIDNIGHT);


        List<ExternalResourceEvent> externalEvents = eventAwareResourceHandler.describeResourceEvents(
                account,
                resource.getExternalId(),
                maxTime
        );

        if(Utils.isEmpty(externalEvents))
            return;

        for (ExternalResourceEvent externalEvent : externalEvents) {
            Optional<ResourceEvent> event = events.stream().filter(
                    e -> e.isSameEvent(resource, externalEvent)
            ).findFirst();

            if(event.isPresent()) {
                event.get().updateByExternal(externalEvent);
                repository.save(event.get());
            } else {
                repository.save(ResourceEvent.from(account, resource, externalEvent));
            }
        }
    }

    private NestedResourceEvent toNestedResourceEvent(ResourceEvent resourceEvent) {
        NestedResourceEvent event = new NestedResourceEvent();

        EntityUtil.copyBasicFields(resourceEvent, event);

        event.setInternalEventId(resourceEvent.getInternalEventId());
        event.setExternalEventId(resourceEvent.getExternalEventId());
        event.setEventType(resourceEvent.getEventType());
        event.setEventTypeName(resourceEvent.getEventTypeName());
        event.setLevel(resourceEvent.getLevel());
        event.setSource(resourceEvent.getSource());
        event.setSummary(resourceEvent.getSummary());
        event.setEventHappenedAt(resourceEvent.getEventHappenedAt());
        event.setEventProperties(resourceEvent.getEventProperties());
        event.setProviderId(resourceEvent.getProviderId());
        event.setProviderName(resourceEvent.getProviderName());
        event.setAccountId(resourceEvent.getAccountId());
        event.setAccountName(resourceEvent.getAccountName());
        event.setResourceCategory(resourceEvent.getResourceCategory());
        event.setResourceCategoryName(resourceEvent.getResourceCategoryName());
        event.setResourceTypeId(resourceEvent.getResourceTypeId());
        event.setResourceTypeName(resourceEvent.getResourceTypeName());
        event.setResourceId(resourceEvent.getResourceId());
        event.setResourceName(resourceEvent.getResourceName());

        return event;
    }
}
