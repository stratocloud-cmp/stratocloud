package com.stratocloud.notification.internal;

import com.stratocloud.config.StratoConfiguration;
import com.stratocloud.notification.NotificationProvider;
import com.stratocloud.notification.NotificationReceiver;
import com.stratocloud.notification.NotificationWay;
import com.stratocloud.notification.NotificationWayProperties;
import com.stratocloud.repository.InternalMailRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
public class InternalMailNotificationProvider implements NotificationProvider {

    private final InternalMailRepository internalMailRepository;

    private final StratoConfiguration stratoConfiguration;

    public InternalMailNotificationProvider(InternalMailRepository internalMailRepository,
                                            StratoConfiguration stratoConfiguration) {
        this.internalMailRepository = internalMailRepository;
        this.stratoConfiguration = stratoConfiguration;
    }

    @Override
    public String getId() {
        return "INTERNAL_MAIL";
    }

    @Override
    public String getName() {
        return "站内信";
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNotification(NotificationReceiver receiver) {
        String renderedMessage = receiver.getRenderedHtmlMessage(stratoConfiguration.getStratoDomainName());
        internalMailRepository.saveWithSystemSession(
                new InternalMail(
                        receiver.getNotification().getEventId(),
                        receiver.getReceiverUserId(),
                        renderedMessage
                )
        );
    }



    @Override
    public Class<? extends NotificationWayProperties> getPropertiesClass() {
        return NotificationWayProperties.Dummy.class;
    }

    @Override
    public void validateConnection(NotificationWay way) {

    }

    @Override
    public void eraseSensitiveInfo(Map<String, Object> properties) {

    }
}
