package com.stratocloud.permission;

import com.stratocloud.identity.IdentityTopics;
import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageConsumer;
import com.stratocloud.repository.PermissionRepository;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
public class InitPermissionConsumer implements MessageConsumer {

    private final PermissionRepository repository;

    public InitPermissionConsumer(PermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void consume(Message message) {
        InitPermissionPayload payload = JSON.toJavaObject(message.getPayload(), InitPermissionPayload.class);

        List<PermissionItem> items = payload.items();
        if(Utils.isEmpty(items))
            return;

        for (PermissionItem item : items) {
            if(repository.existsByTargetAndAction(item.target(), item.action())) {
                log.info("Permission {} already exists.", item);
                continue;
            }

            Permission permission = new Permission(item);
            repository.saveIgnoreDuplicateKey(permission);
        }
    }

    @Override
    public String getTopic() {
        return IdentityTopics.INIT_PERMISSION_TOPIC;
    }

    @Override
    public String getConsumerGroup() {
        return "PERMISSION";
    }
}
