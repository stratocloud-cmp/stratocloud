package com.stratocloud.notification;

import com.stratocloud.auth.RunWithSystemSession;
import com.stratocloud.event.EventNotificationPayload;
import com.stratocloud.event.EventTopics;
import com.stratocloud.event.StratoEventType;
import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageConsumer;
import com.stratocloud.repository.NotificationPolicyRepository;
import com.stratocloud.repository.NotificationRepository;
import com.stratocloud.repository.UserRepository;
import com.stratocloud.user.User;
import com.stratocloud.user.UserFilters;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class EventNotificationConsumer implements MessageConsumer {

    private final NotificationRepository notificationRepository;

    private final NotificationPolicyRepository notificationPolicyRepository;

    private final UserRepository userRepository;

    public EventNotificationConsumer(NotificationRepository notificationRepository,
                                     NotificationPolicyRepository notificationPolicyRepository,
                                     UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.notificationPolicyRepository = notificationPolicyRepository;
        this.userRepository = userRepository;
    }

    @Override
    @RunWithSystemSession
    public void consume(Message message) {
        EventNotificationPayload payload = JSON.toJavaObject(message.getPayload(), EventNotificationPayload.class);

        StratoEventType eventType = payload.eventType();

        List<NotificationPolicy> policies = notificationPolicyRepository.findAllByEventType(eventType.id());

        if(Utils.isEmpty(policies))
            return;

        for (NotificationPolicy policy : policies) {
            try {
                createNotification(policy, payload);
            }catch (Exception e){
                log.warn("Failed to create notification.", e);
            }
        }
    }

    private void createNotification(NotificationPolicy policy, EventNotificationPayload payload) {
        Notification notification = new Notification(
                payload.eventId(),
                payload.eventLevel(),
                payload.eventSource(),
                payload.eventObject(),
                payload.summary(),
                payload.eventHappenedAt(),
                payload.eventProperties(),
                policy
        );

        List<NotificationReceiver> receivers = getReceivers(policy, notification);

        notification.setReceivers(receivers);

        notificationRepository.save(notification);
    }

    private List<NotificationReceiver> getReceivers(NotificationPolicy policy,
                                                    Notification notification) {
        NotificationReceiverType receiverType = policy.getReceiverType();

        UserFilters userFilters = null;
        switch (receiverType) {
            case PRESET_USERS -> {
                if(Utils.isNotEmpty(policy.getPresetUserIds()))
                    userFilters = new UserFilters(
                            null,
                            policy.getPresetUserIds(),
                            null,
                            null,
                            null,
                            false,
                            null
                    );
            }
            case PRESET_ROLES -> {
                if(Utils.isNotEmpty(policy.getPresetRoleIds()))
                    userFilters = new UserFilters(
                            null,
                            null,
                            policy.getPresetRoleIds(),
                            null,
                            null,
                            false,
                            null
                    );
            }
            case PRESET_USER_GROUPS -> {
                if(Utils.isNotEmpty(policy.getPresetUserGroupIds()))
                    userFilters = new UserFilters(
                            null,
                            null,
                            null,
                            policy.getPresetUserGroupIds(),
                            null,
                            false,
                            null
                    );
            }
            case EVENT_OBJECT_OWNER -> {
                if(notification.getEventObjectOwnerId() != null)
                    userFilters = new UserFilters(
                            null,
                            List.of(notification.getEventObjectOwnerId()),
                            null,
                            null,
                            null,
                            false,
                            null
                    );
            }
        }

        List<NotificationReceiver> receivers = new ArrayList<>();

        if(userFilters != null){
            List<User> users = userRepository.findAllByFilters(userFilters);

            if(Utils.isNotEmpty(users)){
                for (User user : users) {
                    NotificationReceiver receiver = new NotificationReceiver(
                            notification,
                            user.getId(),
                            user.getRealName()
                    );

                    receivers.add(receiver);
                }
            }
        }

        return receivers;
    }

    @Override
    public String getTopic() {
        return EventTopics.EVENT_NOTIFICATION_TOPIC;
    }

    @Override
    public String getConsumerGroup() {
        return "NOTIFICATION";
    }
}
