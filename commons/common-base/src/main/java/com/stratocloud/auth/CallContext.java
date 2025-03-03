package com.stratocloud.auth;

import com.stratocloud.identity.BuiltInIds;
import com.stratocloud.exceptions.PermissionNotGrantedException;
import com.stratocloud.exceptions.UnauthorizedException;
import com.stratocloud.identity.RoleType;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class CallContext {

    private String requestId;

    private UserSession userSession;

    private static final ThreadLocal<CallContext> threadLocal = new ThreadLocal<>();

    public static CallContext current(){
        CallContext callContext = threadLocal.get();
        if(callContext==null)
            throw new UnauthorizedException("Unauthorized.");
        return callContext;
    }

    public static boolean exists(){
        return threadLocal.get() != null;
    }

    public static void register(UserSession userSession){
        CallContext callContext = new CallContext();
        callContext.requestId = UUID.randomUUID().toString();
        callContext.userSession = userSession;
        threadLocal.set(callContext);
    }

    public static String requestId(){
        CallContext callContext = threadLocal.get();
        if(callContext==null)
            return UUID.randomUUID().toString();
        return callContext.getRequestId();
    }

    public static void registerSystemSession(){
        UserSession systemSession = getSystemSession();

        register(systemSession);
    }

    public static UserSession getSystemSession() {
        return UserSession.builder()
                .userId(BuiltInIds.SYSTEM_USER_ID)
                .loginName("system")
                .realName("系统")
                .passwordExpired(false)
                .token(UUID.randomUUID().toString())
                .permissions(Map.of())
                .tenantId(BuiltInIds.ROOT_TENANT_ID)
                .grantedTenantIds(List.of(BuiltInIds.ROOT_TENANT_ID))
                .subTenantIds(List.of())
                .inheritedTenantIds(List.of())
                .roleType(RoleType.SUPER_ADMIN)
                .grantedTags(Map.of())
                .build();
    }

    public static CallContext getContext(UserSession userSession){
        CallContext callContext = new CallContext();
        callContext.requestId = UUID.randomUUID().toString();
        callContext.userSession = userSession;
        return callContext;
    }

    public static void unregister(){
        threadLocal.remove();
    }

    public static void registerBack(CallContext callContext) {
        threadLocal.set(callContext);
    }

    public void validatePermission(String target, String action) {
        if(hasPermission(target, action))
            return;

        throw new PermissionNotGrantedException("您没有此权限: " + target + " " + action);
    }


    public UserSession getCallingUser(){
        return userSession;
    }

    public RoleType getRoleType(){
        return userSession.roleType();
    }


    public boolean hasTenantPermission() {
        return getRoleType().ordinal() <= RoleType.TENANT_ADMIN.ordinal();
    }

    public boolean isSuperAdmin() {
        return CallContext.current().getRoleType().ordinal() == RoleType.SUPER_ADMIN.ordinal();
    }

    public boolean isSystemUserCalling(){
        return getCallingUser().isSystemUser();
    }

    public String getRequestId() {
        return requestId;
    }

    public boolean isAdmin() {
        return getRoleType().compareTo(RoleType.NORMAL_USER) < 0;
    }

    public boolean hasPermission(String target, String action) {
        if(userSession.roleType() == RoleType.SUPER_ADMIN || userSession.roleType() == RoleType.TENANT_SUPER_ADMIN)
            return true;

        Map<String, List<String>> permissions = userSession.permissions();

        List<String> permittedActions = permissions.get(target);

        return permittedActions != null && permittedActions.contains(action);
    }
}
