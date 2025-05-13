package com.stratocloud.notification;

import com.stratocloud.auth.RunWithSystemSession;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.lock.DistributedLock;
import com.stratocloud.notification.cmd.ResendNotificationCmd;
import com.stratocloud.notification.query.DescribeNotificationsRequest;
import com.stratocloud.notification.query.NestedNotification;
import com.stratocloud.notification.query.NestedNotificationReceiver;
import com.stratocloud.notification.response.ResendNotificationResponse;
import com.stratocloud.repository.NotificationPolicyRepository;
import com.stratocloud.repository.NotificationRepository;
import com.stratocloud.utils.Utils;
import com.stratocloud.validate.ValidateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService{

    private final NotificationRepository repository;

    private final NotificationPolicyRepository notificationPolicyRepository;

    public NotificationServiceImpl(NotificationRepository repository,
                                   NotificationPolicyRepository notificationPolicyRepository) {
        this.repository = repository;
        this.notificationPolicyRepository = notificationPolicyRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedNotification> describeNotification(DescribeNotificationsRequest request) {
        Page<Notification> page = repository.page(
                request.getSearch(),
                request.getPageable()
        );
        return page.map(this::toNestedNotification);
    }

    private NestedNotification toNestedNotification(Notification notification) {
        NotificationPolicy policy = notification.getPolicy();
        NotificationWay notificationWay = policy.getNotificationWay();

        NestedNotification result = new NestedNotification();

        EntityUtil.copyBasicFields(notification, result);

        result.setEventId(notification.getEventId());
        result.setEventType(notification.getEventType());
        result.setEventTypeName(notification.getEventTypeName());
        result.setEventLevel(notification.getEventLevel());
        result.setEventSource(notification.getEventSource());
        result.setEventObjectType(notification.getEventObjectType());
        result.setEventObjectTypeName(notification.getEventObjectTypeName());
        result.setEventObjectId(notification.getEventObjectId());
        result.setEventObjectName(notification.getEventObjectName());
        result.setEventSummary(notification.getEventSummary());
        result.setEventHappenedAt(notification.getEventHappenedAt());

        result.setEventProperties(notification.getEventProperties());
        result.setSentCount(notification.getSentCount());
        result.setLastSentTime(notification.getLastSentTime());
        result.setNotificationPolicyId(policy.getId());
        result.setNotificationPolicyName(policy.getName());
        result.setEventType(policy.getEventType().getEventType());
        result.setEventTypeName(policy.getEventType().getEventTypeName());
        result.setNotificationWayId(notificationWay.getId());
        result.setNotificationWayName(notificationWay.getName());
        result.setReceivers(toNestedReceivers(notification.getReceivers()));

        return result;
    }

    private List<NestedNotificationReceiver> toNestedReceivers(List<NotificationReceiver> receivers) {
        List<NestedNotificationReceiver> result = new ArrayList<>();

        if(Utils.isNotEmpty(receivers)){
            for (NotificationReceiver receiver : receivers) {
                NestedNotificationReceiver nestedReceiver = new NestedNotificationReceiver();

                EntityUtil.copyBasicFields(receiver, nestedReceiver);

                nestedReceiver.setReceiverUserId(receiver.getReceiverUserId());
                nestedReceiver.setReceiverUserRealName(receiver.getReceiverUserRealName());
                nestedReceiver.setSuccessfullySentCount(receiver.getSuccessfullySentCount());
                nestedReceiver.setState(receiver.getState());
                nestedReceiver.setErrorMessage(receiver.getErrorMessage());

                result.add(nestedReceiver);
            }
        }

        return result;
    }

    @Override
    @ValidateRequest
    @Transactional
    public ResendNotificationResponse resendNotification(ResendNotificationCmd cmd) {
        Notification notification = repository.findNotification(cmd.getNotificationId());

        notification.send(cmd.getReceiverUserIds());

        repository.save(notification);

        return new ResendNotificationResponse();
    }


    @Scheduled(fixedDelay = 10L, timeUnit = TimeUnit.SECONDS)
    @DistributedLock(lockName = "SEND_NOTIFICATIONS_SCHEDULED_JOB", waitIfLocked = false)
    @RunWithSystemSession
    public void sendNotifications() {
        List<NotificationPolicy> policies = notificationPolicyRepository.findAll();

        if(Utils.isEmpty(policies))
            return;

        for (NotificationPolicy policy : policies) {
            try {
                ((NotificationServiceImpl) AopContext.currentProxy()).sendNotifications(policy.getId());
            }catch (Exception e){
                log.warn(e.toString());
            }
        }
    }

    @Transactional
    public void sendNotifications(Long policyId) {
        NotificationPolicy policy = notificationPolicyRepository.findNotificationPolicy(policyId);
        List<Notification> notifications = repository.findByPolicyIdAndSentCountLessThan(
                policy.getId(), policy.getMaxNotificationTimes()
        );

        if(Utils.isEmpty(notifications))
            return;

        int intervalMinutes = policy.getNotificationIntervalMinutes();

        for (Notification notification : notifications) {
            try {
                LocalDateTime lastSentTime = notification.getLastSentTime();
                LocalDateTime threshold = LocalDateTime.now().minusMinutes(intervalMinutes);
                if(lastSentTime != null && lastSentTime.isAfter(threshold))
                    continue;

                notification.sendToAll();
            }catch (Exception e){
                log.warn(e.toString());
            }finally {
                repository.save(notification);
            }
        }
    }
}
