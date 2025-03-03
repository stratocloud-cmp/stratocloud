package com.stratocloud.jpa.consumers;

import com.stratocloud.identity.BuiltInIds;
import com.stratocloud.identity.IdentityTopics;
import com.stratocloud.identity.TenantDeletedPayload;
import com.stratocloud.jpa.entities.Tenanted;
import com.stratocloud.jpa.repository.Repository;
import com.stratocloud.jpa.repository.TenantedRepository;
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
public class TenantDeletedConsumer implements MessageConsumer {


    @Override
    @Transactional
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void consume(Message message) {
        ApplicationContext applicationContext = ContextUtil.getApplicationContext();
        var repositories = applicationContext.getBeansOfType(Repository.class).values();

        TenantDeletedPayload payload = JSON.toJavaObject(message.getPayload(), TenantDeletedPayload.class);

        for (Repository<?,?> repository : repositories)
            if(repository instanceof TenantedRepository tenantedRepository)
                handleTenantDeleted(tenantedRepository, payload);
    }

    private void handleTenantDeleted(TenantedRepository<Tenanted> tenantedRepository,
                                     TenantDeletedPayload payload) {
        if(!tenantedRepository.transferWhenTenantDeleted())
            return;

        List<Long> tenantIds = List.of(payload.tenant().id());
        List<Tenanted> tenantedEntities = tenantedRepository.findAllByTenantIds(tenantIds);

        if(Utils.isEmpty(tenantedEntities))
            return;

        int size = tenantedEntities.size();
        String className = tenantedEntities.get(0).getClass().getSimpleName();
        String tenantName = payload.tenant().name();
        log.info("Transferring {} entities of {} in deleted tenant {} to root tenant.", size, className, tenantName);

        for (Tenanted tenantedEntity : tenantedEntities) {
            tenantedEntity.transferToNewTenant(BuiltInIds.ROOT_TENANT_ID);
            log.info("Transferring entity {} to root tenant.", tenantedEntity);
        }

        tenantedRepository.saveAll(tenantedEntities);
    }

    @Override
    public String getTopic() {
        return IdentityTopics.TENANT_DELETED_TOPIC;
    }

    @Override
    public String getConsumerGroup() {
        return "MAIN";
    }
}
