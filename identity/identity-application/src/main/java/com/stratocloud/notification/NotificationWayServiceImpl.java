package com.stratocloud.notification;

import com.stratocloud.auth.RunWithSystemSession;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.lock.DistributedLock;
import com.stratocloud.notification.cmd.CreateNotificationWayCmd;
import com.stratocloud.notification.cmd.DeleteNotificationWaysCmd;
import com.stratocloud.notification.cmd.UpdateNotificationWayCmd;
import com.stratocloud.notification.query.*;
import com.stratocloud.notification.response.CreateNotificationWayResponse;
import com.stratocloud.notification.response.DeleteNotificationWaysResponse;
import com.stratocloud.notification.response.UpdateNotificationWayResponse;
import com.stratocloud.repository.NotificationWayRepository;
import com.stratocloud.utils.Utils;
import com.stratocloud.validate.ValidateRequest;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class NotificationWayServiceImpl implements NotificationWayService {

    private final NotificationWayRepository repository;

    public NotificationWayServiceImpl(NotificationWayRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    @ValidateRequest
    public CreateNotificationWayResponse createNotificationWay(CreateNotificationWayCmd cmd) {
        NotificationWay notificationWay = new NotificationWay(
                cmd.getProviderId(),
                cmd.getName(),
                cmd.getDescription(),
                cmd.getProperties()
        );

        if(cmd.getTenantId() != null)
            notificationWay.setTenantId(cmd.getTenantId());

        notificationWay.validateConnection();

        notificationWay.setProviderStatus(NotificationProviderStatus.NORMAL);

        notificationWay = repository.save(notificationWay);

        return new CreateNotificationWayResponse(notificationWay.getId());
    }

    @Override
    @Transactional
    @ValidateRequest
    public UpdateNotificationWayResponse updateNotificationWay(UpdateNotificationWayCmd cmd) {
        NotificationWay notificationWay = repository.findNotificationWay(cmd.getNotificationWayId());

        notificationWay.update(
                cmd.getName(),
                cmd.getDescription(),
                cmd.getProperties()
        );

        notificationWay.validateConnection();

        repository.save(notificationWay);

        return new UpdateNotificationWayResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public DeleteNotificationWaysResponse deleteNotificationWays(DeleteNotificationWaysCmd cmd) {
        List<Long> wayIds = cmd.getNotificationWayIds();
        boolean force = cmd.isForce();

        if(Utils.isNotEmpty(wayIds))
            wayIds.forEach(wayId -> deleteNotificationWay(wayId, force));

        return new DeleteNotificationWaysResponse();
    }

    private void deleteNotificationWay(Long wayId, boolean force) {
        NotificationWay notificationWay = repository.findNotificationWay(wayId);

        if(!force)
            if(Utils.isNotEmpty(notificationWay.getPolicies()))
                throw new BadCommandException("该通知方式下存在通知策略，无法删除");


        repository.delete(notificationWay);
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedNotificationWay> describeNotificationWays(DescribeNotificationWaysRequest request) {
        Page<NotificationWay> page = repository.page(
                request.getSearch(),
                request.getPageable()
        );

        return page.map(this::toNestedNotificationWay);
    }

    @Override
    public DescribeNotificationProvidersResponse describeNotificationProviders(DescribeNotificationProvidersRequest request) {
        var list = NotificationProviderRegistry.getProviders().stream().map(
                this::toNestedNotificationProvider
        ).toList();

        return new DescribeNotificationProvidersResponse(list);
    }

    private NestedNotificationProvider toNestedNotificationProvider(NotificationProvider provider) {
        NestedNotificationProvider result = new NestedNotificationProvider();
        result.setProviderId(provider.getId());
        result.setProviderName(provider.getName());
        result.setFormMetaData(DynamicFormHelper.generateMetaData(provider.getPropertiesClass()));
        return result;
    }

    private NestedNotificationWay toNestedNotificationWay(NotificationWay way) {
        NotificationProvider provider = way.getProvider();

        NestedNotificationWay result = new NestedNotificationWay();

        EntityUtil.copyBasicFields(way, result);

        result.setProviderId(provider.getId());
        result.setProviderName(provider.getName());

        result.setName(way.getName());
        result.setDescription(way.getDescription());

        Map<String, Object> properties = way.getProperties();
        provider.eraseSensitiveInfo(properties);
        result.setProperties(properties);

        result.setProviderStatus(way.getProviderStatus());
        result.setErrorMessage(way.getErrorMessage());

        return result;
    }

    @RunWithSystemSession
    @DistributedLock(lockName = "CHECK_NOTIFICATION_WAYS_STATE_SCHEDULED_JOB", waitIfLocked = false)
    @Scheduled(fixedDelay = 60L, timeUnit = TimeUnit.MINUTES)
    @Transactional
    public void checkNotificationWaysState(){
        List<NotificationWay> notificationWays = repository.findAll();

        if(Utils.isEmpty(notificationWays))
            return;

        notificationWays.forEach(NotificationWay::checkConnectionQuietly);

        repository.saveAll(notificationWays);
    }
}
