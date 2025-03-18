package com.stratocloud.provider.dynamic;

import com.stratocloud.identity.IdentityTopics;
import com.stratocloud.lock.DistributedLock;
import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageBus;
import com.stratocloud.permission.InitPermissionPayload;
import com.stratocloud.permission.PermissionItem;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.utils.ContextUtil;
import com.stratocloud.utils.Utils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class DynamicResourcePermissionsEnsured {

    private final MessageBus messageBus;

    private final Set<PermissionItem> ensuredPermissionItems = new HashSet<>();

    public DynamicResourcePermissionsEnsured(MessageBus messageBus) {
        this.messageBus = messageBus;
    }

    @DistributedLock(lockName = "DYNAMIC_RESOURCE_PERMISSION_ENSURED", waitIfLocked = false)
    @Scheduled(fixedDelay = 30L, timeUnit = TimeUnit.SECONDS)
    public void ensurePermissions(){
        var beansOfType = ContextUtil.getApplicationContext().getBeansOfType(
                DynamicResourceHandlerLoader.class
        );

        if(Utils.isEmpty(beansOfType))
            return;

        Set<PermissionItem> permissionItems = new HashSet<>();

        for (DynamicResourceHandlerLoader loader : beansOfType.values()) {
            for (DynamicResourceHandler resourceHandler : loader.loadResourceHandlers()) {
                if(!ensuredPermissionItems.contains(resourceHandler.getPermissionItem()))
                    permissionItems.add(resourceHandler.getPermissionItem());
                for (ResourceActionHandler actionHandler : resourceHandler.getActionHandlers()) {
                    if(!ensuredPermissionItems.contains(resourceHandler.getPermissionItem()))
                        permissionItems.add(actionHandler.getPermissionItem());
                }
            }
        }

        messageBus.publishWithSystemSession(Message.create(
                IdentityTopics.INIT_PERMISSION_TOPIC,
                new InitPermissionPayload(new ArrayList<>(permissionItems))
        ));

        ensuredPermissionItems.addAll(permissionItems);
    }
}
