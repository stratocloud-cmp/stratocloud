package com.stratocloud.notification;

import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.notification.cmd.CreateNotificationPolicyCmd;
import com.stratocloud.notification.cmd.DeleteNotificationPoliciesCmd;
import com.stratocloud.notification.cmd.UpdateNotificationPolicyCmd;
import com.stratocloud.notification.query.*;
import com.stratocloud.notification.response.CreateNotificationPolicyResponse;
import com.stratocloud.notification.response.DeleteNotificationPoliciesResponse;
import com.stratocloud.notification.response.UpdateNotificationPolicyResponse;
import com.stratocloud.repository.NotificationEventTypeRepository;
import com.stratocloud.repository.NotificationPolicyRepository;
import com.stratocloud.repository.NotificationWayRepository;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.stratocloud.validate.ValidateRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@Service
public class NotificationPolicyServiceImpl implements NotificationPolicyService {

    private final NotificationPolicyRepository repository;

    private final NotificationEventTypeRepository eventTypeRepository;

    private final NotificationWayRepository notificationWayRepository;

    public NotificationPolicyServiceImpl(NotificationPolicyRepository repository,
                                         NotificationEventTypeRepository eventTypeRepository,
                                         NotificationWayRepository notificationWayRepository) {
        this.repository = repository;
        this.eventTypeRepository = eventTypeRepository;
        this.notificationWayRepository = notificationWayRepository;
    }

    @Override
    @Transactional
    @ValidateRequest
    public CreateNotificationPolicyResponse createNotificationPolicy(CreateNotificationPolicyCmd cmd) {
        NotificationEventType eventType = eventTypeRepository.findByEventType(cmd.getEventType());

        NotificationWay notificationWay = notificationWayRepository.findNotificationWay(cmd.getNotificationWayId());

        NotificationPolicy notificationPolicy = new NotificationPolicy(
                eventType,
                cmd.getPolicyKey(),
                cmd.getName(),
                cmd.getDescription(),
                cmd.getReceiverType(),
                cmd.getPresetUserIds(),
                cmd.getPresetUserGroupIds(),
                cmd.getPresetRoleIds(),
                notificationWay,
                cmd.getTemplate(),
                cmd.getMaxNotificationTimes(),
                cmd.getNotificationIntervalMinutes()
        );

        if(cmd.getTenantId() != null)
            notificationPolicy.setTenantId(cmd.getTenantId());

        notificationPolicy = repository.save(notificationPolicy);

        return new CreateNotificationPolicyResponse(notificationPolicy.getId());
    }

    @Override
    @Transactional
    @ValidateRequest
    public UpdateNotificationPolicyResponse updateNotificationPolicy(UpdateNotificationPolicyCmd cmd) {
        NotificationPolicy policy = repository.findNotificationPolicy(cmd.getNotificationPolicyId());

        policy.update(
                cmd.getName(),
                cmd.getDescription(),
                cmd.getReceiverType(),
                cmd.getPresetUserIds(),
                cmd.getPresetUserGroupIds(),
                cmd.getPresetRoleIds(),
                cmd.getTemplate(),
                cmd.getMaxNotificationTimes(),
                cmd.getNotificationIntervalMinutes()
        );

        repository.save(policy);

        return new UpdateNotificationPolicyResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public DeleteNotificationPoliciesResponse deleteNotificationPolicies(DeleteNotificationPoliciesCmd cmd) {

        if(Utils.isNotEmpty(cmd.getNotificationPolicyIds()))
            cmd.getNotificationPolicyIds().forEach(
                    policyId -> deleteNotificationPolicy(policyId, cmd.isForce())
            );

        return new DeleteNotificationPoliciesResponse();
    }

    private void deleteNotificationPolicy(Long policyId, boolean force) {
        NotificationPolicy policy = repository.findNotificationPolicy(policyId);

        if(!force)
            if(Utils.isNotEmpty(policy.getNotifications()))
                throw new BadCommandException("该通知策略下仍有通知记录存在，无法删除");

        repository.delete(policy);
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedNotificationPolicy> describeNotificationPolicies(DescribeNotificationPoliciesRequest request) {
        Page<NotificationPolicy> page = repository.page(
                request.getSearch(),
                request.getPageable()
        );

        return page.map(this::toNestedNotificationPolicy);
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public DescribeNotificationEventTypesResponse describeNotificationEventTypes(DescribeNotificationEventTypesRequest request) {
        var list = eventTypeRepository.findAll().stream().map(
                this::toNestedNotificationEventType
        ).sorted(
                Comparator.comparing(NestedNotificationEventType::getEventType)
        ).toList();
        return new DescribeNotificationEventTypesResponse(list);
    }

    private NestedNotificationEventType toNestedNotificationEventType(NotificationEventType notificationEventType) {
        NestedNotificationEventType result = new NestedNotificationEventType();

        EntityUtil.copyBasicFields(notificationEventType, result);

        result.setEventType(notificationEventType.getEventType());
        result.setEventTypeName(notificationEventType.getEventTypeName());
        result.setEventPropertiesExample(JSON.toPrettyJsonString(notificationEventType.getEventPropertiesExample()));

        return result;
    }

    private NestedNotificationPolicy toNestedNotificationPolicy(NotificationPolicy policy) {
        NestedNotificationPolicy result = new NestedNotificationPolicy();

        EntityUtil.copyBasicFields(policy, result);

        result.setEventType(policy.getEventType().getEventType());
        result.setEventTypeName(policy.getEventType().getEventTypeName());
        result.setPolicyKey(policy.getPolicyKey());
        result.setName(policy.getName());
        result.setDescription(policy.getDescription());
        result.setReceiverType(policy.getReceiverType());
        result.setPresetUserIds(policy.getPresetUserIds());
        result.setPresetUserGroupIds(policy.getPresetUserGroupIds());
        result.setPresetRoleIds(policy.getPresetRoleIds());
        result.setNotificationWayId(policy.getNotificationWay().getId());
        result.setNotificationWayName(policy.getNotificationWay().getName());
        result.setTemplate(policy.getTemplate());
        result.setMaxNotificationTimes(policy.getMaxNotificationTimes());
        result.setNotificationIntervalMinutes(policy.getNotificationIntervalMinutes());

        return result;
    }
}
