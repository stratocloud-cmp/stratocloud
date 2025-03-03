package com.stratocloud.audit;

import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageConsumer;
import com.stratocloud.repository.AuditLogRepository;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

@Component
public class AuditLogConsumer implements MessageConsumer {

    private final AuditLogRepository repository;

    public AuditLogConsumer(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void consume(Message message) {
        String payload = message.getPayload();
        AuditLogPayload auditLogPayload = JSON.toJavaObject(payload, AuditLogPayload.class);

        AuditLog auditLog = new AuditLog(auditLogPayload);

        repository.save(auditLog);
    }

    @Override
    public String getTopic() {
        return "SEND_AUDIT_LOG";
    }

    @Override
    public String getConsumerGroup() {
        return "AUDIT_LOG";
    }
}
