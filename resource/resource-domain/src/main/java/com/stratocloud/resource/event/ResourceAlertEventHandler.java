package com.stratocloud.resource.event;

import com.stratocloud.event.*;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageBus;
import com.stratocloud.provider.resource.monitor.MetricsProvider;
import com.stratocloud.provider.resource.monitor.SupportedMetric;
import com.stratocloud.repository.ResourceEventRepository;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class ResourceAlertEventHandler implements EventHandler<ResourceAlertEventProperties> {

    private final MessageBus messageBus;

    private final ResourceEventRepository eventRepository;

    public ResourceAlertEventHandler(MessageBus messageBus, ResourceEventRepository eventRepository) {
        this.messageBus = messageBus;
        this.eventRepository = eventRepository;
    }

    @Override
    public void handleEvent(StratoEvent<ResourceAlertEventProperties> event) {
        try {
            ResourceEvent resourceEvent = ResourceEvent.from(event);
            eventRepository.save(resourceEvent);

            messageBus.publishWithSystemSession(
                    Message.create(
                            EventTopics.EVENT_NOTIFICATION_TOPIC,
                            EventNotificationPayload.from(event)
                    )
            );
        }catch (Exception e){
            log.warn("Failed to handle resource action event.", e);
        }
    }

    @Override
    public Set<StratoEventType> getSupportedEventTypes(ApplicationContext applicationContext) {
        Map<String, MetricsProvider> providerMap = applicationContext.getBeansOfType(MetricsProvider.class);

        Set<StratoEventType> result = new HashSet<>();

        if(Utils.isEmpty(providerMap))
            return result;

        for (MetricsProvider metricsProvider : providerMap.values()) {
            for (SupportedMetric supportedMetric : metricsProvider.getSupportedMetrics()) {
                supportedMetric.alertEventType().ifPresent(result::add);
                supportedMetric.alertRecoveredEventType().ifPresent(result::add);
            }
        }

        return result;
    }

    @Override
    public ResourceAlertEventProperties getExampleEventProperties() {
        return ResourceAlertEventProperties.createExample();
    }

    @Override
    public List<BuiltInNotificationPolicy> getBuiltInNotificationPolicies(ApplicationContext applicationContext) {
        return getSupportedEventTypes(applicationContext).stream().map(
                t -> new BuiltInNotificationPolicy(
                        t,
                        "%s_NOTIFICATION".formatted(t.id()),
                        t.name(),
                        null,
                        "EVENT_OBJECT_OWNER",
                        null,
                        null,
                        null,
                        new BuiltInNotificationWay(
                                "INTERNAL_MAIL","站内信",null, null
                        ),
                        loadTemplate(applicationContext),
                        1,
                        5
                )
        ).toList();
    }

    private String loadTemplate(ApplicationContext applicationContext){
        Resource resource = applicationContext.getResource(
                "classpath:templates/ResourceAlertNotification.html"
        );

        try {
            return IOUtils.toString(resource.getURI(), Charset.defaultCharset());
        } catch (IOException e) {
            throw new StratoException(e.getMessage(), e);
        }
    }
}
