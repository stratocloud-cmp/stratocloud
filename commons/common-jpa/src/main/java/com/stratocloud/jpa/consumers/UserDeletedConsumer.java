package com.stratocloud.jpa.consumers;

import com.stratocloud.identity.BuiltInIds;
import com.stratocloud.identity.IdentityTopics;
import com.stratocloud.identity.UserDeletedPayload;
import com.stratocloud.jpa.entities.Controllable;
import com.stratocloud.jpa.repository.ControllableRepository;
import com.stratocloud.jpa.repository.Repository;
import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageConsumer;
import com.stratocloud.utils.ContextUtil;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
public class UserDeletedConsumer implements MessageConsumer {


    @Override
    @Transactional
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void consume(Message message) {
        ApplicationContext applicationContext = ContextUtil.getApplicationContext();
        var repositories = applicationContext.getBeansOfType(Repository.class).values();

        UserDeletedPayload payload = JSON.toJavaObject(message.getPayload(), UserDeletedPayload.class);

        for (Repository<?,?> repository : repositories)
            if(repository instanceof ControllableRepository tenantedRepository)
                handleUserDeleted(tenantedRepository, payload);
    }

    private void handleUserDeleted(ControllableRepository<Controllable> controllableRepository,
                                   UserDeletedPayload payload) {
        if(!controllableRepository.transferWhenOwnerDeleted())
            return;

        List<Long> userIds = List.of(payload.user().userId());
        List<Controllable> controlledEntities = controllableRepository.findByOwnerIds(userIds);

        if(Utils.isEmpty(controlledEntities))
            return;

        int size = controlledEntities.size();
        String className = controlledEntities.get(0).getClass().getSimpleName();
        String loginName = payload.user().loginName();
        log.info("Transferring {} entities of {} owned by deleted user {} to system.", size, className, loginName);

        for (Controllable controllable : controlledEntities) {
            controllable.transferToNewOwner(BuiltInIds.SYSTEM_USER_ID);
            log.info("Transferring entity {} to system.", controllable);
        }

        controllableRepository.saveAll(controlledEntities);
    }

    @Override
    public String getTopic() {
        return IdentityTopics.USER_DELETED_TOPIC;
    }

    @Override
    public String getConsumerGroup() {
        return "MAIN";
    }
}
