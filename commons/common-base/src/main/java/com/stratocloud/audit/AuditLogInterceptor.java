package com.stratocloud.audit;

import com.stratocloud.auth.CallContext;
import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageBus;
import com.stratocloud.request.RequestRecord;
import com.stratocloud.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class AuditLogInterceptor implements HandlerInterceptor {

    private final String sourceIpHeader;

    private final MessageBus messageBus;

    public AuditLogInterceptor(@Value("${strato.audit.sourceIpHeader:}") String sourceIpHeader,
                               MessageBus messageBus) {
        this.sourceIpHeader = sourceIpHeader;
        this.messageBus = messageBus;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) {
        handleAuditLog(request, response, handler, Set.of(AuditLogLevel.info));
    }

    private void handleAuditLog(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Set<AuditLogLevel> handledLogLevels) {
        if(handler instanceof HandlerMethod handlerMethod){
            SendAuditLog sendAuditLog = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getMethod(), SendAuditLog.class
            );

            if(sendAuditLog == null)
                return;

            int statusCode = response.getStatus();
            AuditLogLevel level;

            level = getAuditLogLevel(statusCode);

            if(!handledLogLevels.contains(level))
                return;

            CallContext callContext;
            if(CallContext.exists())
                callContext = CallContext.current();
            else if(AuditLogContext.current().getUserSession() != null)
                callContext = CallContext.getContext(AuditLogContext.current().getUserSession());
            else
                callContext = CallContext.getContext(CallContext.getSystemSession());


            String action = sendAuditLog.action();
            String actionName = sendAuditLog.actionName();
            String objectType = sendAuditLog.objectType();
            String objectTypeName = sendAuditLog.objectTypeName();

            RequestRecord requestRecord = RequestRecord.build(
                    request,
                    response,
                    callContext,
                    sourceIpHeader,
                    sendAuditLog.hideRequestBody(),
                    sendAuditLog.hideResponseBody()
            );

            AuditLogContext auditLogContext = AuditLogContext.current();
            List<AuditObject> auditObjects = auditLogContext.getAuditObjects();
            String specificAction = auditLogContext.getSpecificAction();
            String specificActionName = auditLogContext.getSpecificActionName();
            String specificObjectType = auditLogContext.getSpecificObjectType();
            String specificObjectTypeName = auditLogContext.getSpecificObjectTypeName();
            AuditLogContext.remove();

            if(Utils.isNotBlank(specificAction) && Utils.isNotBlank(specificActionName)) {
                action = specificAction;
                actionName = specificActionName;
            }

            if(Utils.isNotBlank(specificObjectType) && Utils.isNotBlank(specificObjectTypeName)) {
                objectType = specificObjectType;
                objectTypeName = specificObjectTypeName;
            }

            List<String> objectIds = auditObjects.stream().map(AuditObject::id).toList();
            List<String> objectNames = auditObjects.stream().map(AuditObject::name).toList();

            AuditLogPayload payload = new AuditLogPayload(
                    action,
                    actionName,
                    objectType,
                    objectTypeName,
                    objectIds,
                    objectNames,
                    level,
                    requestRecord
            );

            messageBus.publishWithSystemSession(
                    Message.create(
                            "SEND_AUDIT_LOG",
                            payload
                    )
            );
        }
    }

    private static AuditLogLevel getAuditLogLevel(int statusCode) {
        AuditLogLevel level;
        if(statusCode >= 400 && statusCode < 500)
            level = AuditLogLevel.warning;
        else if(statusCode >= 500)
            level = AuditLogLevel.error;
        else
            level = AuditLogLevel.info;
        return level;
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        handleAuditLog(request, response, handler, Set.of(AuditLogLevel.warning, AuditLogLevel.error));
    }
}
