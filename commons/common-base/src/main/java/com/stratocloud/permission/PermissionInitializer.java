package com.stratocloud.permission;

import com.stratocloud.auth.CallContext;
import com.stratocloud.identity.IdentityTopics;
import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageBus;
import com.stratocloud.utils.Utils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.lang.reflect.Method;
import java.util.*;

@Component
public class PermissionInitializer implements ApplicationContextAware {

    private final MessageBus messageBus;


    public PermissionInitializer(MessageBus messageBus) {
        this.messageBus = messageBus;
    }


    private void publish(InitPermissionPayload payload){
        Message message = Message.create(IdentityTopics.INIT_PERMISSION_TOPIC, payload);
        messageBus.publishWithSystemSession(message);
    }

    private Set<PermissionItem> getDynamicPermission(ApplicationContext applicationContext) {
        Map<String, DynamicPermissionRequired> beansMap
                = applicationContext.getBeansOfType(DynamicPermissionRequired.class);

        Set<PermissionItem> permissionItems = new HashSet<>();

        if(Utils.isEmpty(beansMap))
            return permissionItems;

        for (DynamicPermissionRequired dynamicPermissionRequired : beansMap.values()) {
            permissionItems.add(dynamicPermissionRequired.getPermissionItem());
        }

        return permissionItems;
    }

    private Set<PermissionItem> getStaticPermission(ApplicationContext applicationContext) {
        Map<String, Object> controllersMap = applicationContext.getBeansWithAnnotation(Controller.class);

        Set<PermissionItem> permissionItems = new HashSet<>();

        if(Utils.isEmpty(controllersMap))
            return permissionItems;

        for (Object controller : controllersMap.values()) {
            permissionItems.addAll(getPermissionItems(controller.getClass()));
        }

        return permissionItems;
    }

    private static List<PermissionItem> getPermissionItems(Class<?> controllerClass) {
        List<PermissionItem> items = new ArrayList<>();

        PermissionTarget target = AnnotatedElementUtils.findMergedAnnotation(controllerClass, PermissionTarget.class);

        if(target == null)
            return items;


        for (Method method : controllerClass.getMethods()) {
            PermissionRequired permissionRequired
                    = AnnotatedElementUtils.findMergedAnnotation(method, PermissionRequired.class);

            if(permissionRequired==null)
                continue;

            PermissionItem permissionItem = new PermissionItem(
                    target.target(), target.targetName(), permissionRequired.action(), permissionRequired.actionName()
            );
            items.add(permissionItem);
        }

        return items;
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        CallContext.registerSystemSession();

        Set<PermissionItem> permissionItems = new HashSet<>();

        permissionItems.addAll(getStaticPermission(applicationContext));
        permissionItems.addAll(getDynamicPermission(applicationContext));

        InitPermissionPayload payload = new InitPermissionPayload(new ArrayList<>(permissionItems));
        publish(payload);

        CallContext.unregister();
    }
}
