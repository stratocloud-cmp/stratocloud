package com.stratocloud.notification;

import com.stratocloud.event.*;
import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageConsumer;
import com.stratocloud.repository.NotificationEventTypeRepository;
import com.stratocloud.repository.NotificationPolicyRepository;
import com.stratocloud.repository.NotificationWayRepository;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class InitEventTypeConsumer implements MessageConsumer {

    private final NotificationEventTypeRepository eventTypeRepository;

    private final NotificationWayRepository notificationWayRepository;

    private final NotificationPolicyRepository notificationPolicyRepository;

    public InitEventTypeConsumer(NotificationEventTypeRepository eventTypeRepository,
                                 NotificationWayRepository notificationWayRepository,
                                 NotificationPolicyRepository notificationPolicyRepository) {
        this.eventTypeRepository = eventTypeRepository;
        this.notificationWayRepository = notificationWayRepository;
        this.notificationPolicyRepository = notificationPolicyRepository;
    }

    @Override
    @Transactional
    public void consume(Message message) {
        InitEventTypesPayload payload = JSON.toJavaObject(message.getPayload(), InitEventTypesPayload.class);

        if(Utils.isNotEmpty(payload.eventTypes())){
            for (StratoEventType eventType : payload.eventTypes()) {
                if(eventTypeRepository.existsByEventType(eventType.id()))
                    continue;

                NotificationEventType notificationEventType = new NotificationEventType(
                        eventType.id(),
                        eventType.name(),
                        payload.eventPropertiesExample()
                );

                eventTypeRepository.saveIgnoreDuplicateKey(notificationEventType);
            }
        }

        if(Utils.isNotEmpty(payload.builtInNotificationPolicies())){
            for (BuiltInNotificationPolicy policy : payload.builtInNotificationPolicies()) {
                if(notificationPolicyRepository.existsByPolicyKey(policy.policyKey()))
                    continue;

                NotificationEventType eventType = eventTypeRepository.findByEventType(policy.eventType().id());

                BuiltInNotificationWay builtInNotificationWay = policy.notificationWay();

                List<NotificationWay> notificationWays = notificationWayRepository.findAll();

                Optional<NotificationWay> existedWay = notificationWays.stream().filter(
                        w -> Objects.equals(
                                w.getProviderId(),
                                builtInNotificationWay.providerId()
                        )
                ).findFirst();

                NotificationWay notificationWay;

                notificationWay = existedWay.orElse(
                        notificationWayRepository.save(
                                new NotificationWay(
                                        builtInNotificationWay.providerId(),
                                        builtInNotificationWay.name(),
                                        builtInNotificationWay.description(),
                                        builtInNotificationWay.properties()
                                )
                        )
                );

                NotificationReceiverType receiverType;
                if(Utils.isNotEmpty(policy.presetUserIds()))
                    receiverType = NotificationReceiverType.PRESET_USERS;
                else if(Utils.isNotEmpty(policy.presetRoleIds()))
                    receiverType = NotificationReceiverType.PRESET_ROLES;
                else if(Utils.isNotEmpty(policy.presetUserGroupIds()))
                    receiverType = NotificationReceiverType.PRESET_USER_GROUPS;
                else
                    receiverType = NotificationReceiverType.EVENT_OBJECT_OWNER;

                NotificationPolicy notificationPolicy = new NotificationPolicy(
                        eventType,
                        policy.policyKey(),
                        policy.name(),
                        policy.description(),
                        receiverType,
                        policy.presetUserIds(),
                        policy.presetUserGroupIds(),
                        policy.presetRoleIds(),
                        notificationWay,
                        policy.template(),
                        policy.maxNotificationTimes(),
                        policy.notificationIntervalMinutes()
                );

                notificationPolicyRepository.saveIgnoreDuplicateKey(notificationPolicy);
            }
        }
    }

    @Override
    public String getTopic() {
        return EventTopics.EVENT_TYPES_INITIALIZE_TOPIC;
    }

    @Override
    public String getConsumerGroup() {
        return "NOTIFICATION";
    }
}
