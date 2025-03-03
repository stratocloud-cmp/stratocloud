package com.stratocloud.audit;

import com.stratocloud.auth.UserSession;

import java.util.ArrayList;
import java.util.List;

public class AuditLogContext {

    private static final ThreadLocal<AuditLogContext> threadLocal = new ThreadLocal<>();

    private final List<AuditObject> auditObjects = new ArrayList<>();

    private String specificAction;
    private String specificActionName;

    private String specificObjectType;
    private String specificObjectTypeName;

    private UserSession userSession;

    public static boolean exists(){
        return threadLocal.get() != null;
    }

    public static AuditLogContext current() {
        AuditLogContext context = threadLocal.get();

        if(context == null) {
            context = new AuditLogContext();
            threadLocal.set(context);
        }

        return context;
    }

    public void addAuditObject(AuditObject auditObject){
        if(auditObjects.stream().anyMatch(o -> o.id().equals(auditObject.id())))
            return;

        auditObjects.add(auditObject);
    }

    public List<AuditObject> getAuditObjects() {
        return auditObjects;
    }

    public static void remove(){
        threadLocal.remove();
    }

    public void setSpecificAction(String specificAction, String specificActionName){
        this.specificAction = specificAction;
        this.specificActionName = specificActionName;
    }

    public void setSpecificObjectType(String specificObjectType, String specificObjectTypeName){
        this.specificObjectType = specificObjectType;
        this.specificObjectTypeName = specificObjectTypeName;
    }

    public String getSpecificAction() {
        return specificAction;
    }

    public String getSpecificActionName() {
        return specificActionName;
    }

    public String getSpecificObjectType() {
        return specificObjectType;
    }

    public String getSpecificObjectTypeName() {
        return specificObjectTypeName;
    }

    public UserSession getUserSession() {
        return userSession;
    }

    public void setUserSession(UserSession userSession) {
        this.userSession = userSession;
    }
}
