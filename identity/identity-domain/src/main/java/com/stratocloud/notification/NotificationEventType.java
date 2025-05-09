package com.stratocloud.notification;

import com.stratocloud.jpa.entities.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(name = "unique_idx_event_type", columnNames = "event_type"))
public class NotificationEventType extends Auditable {
    @Column(nullable = false)
    private String eventType;
    @Column(nullable = false)
    private String eventTypeName;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> eventPropertiesExample;

    public NotificationEventType(String eventType,
                                 String eventTypeName,
                                 Map<String, Object> eventPropertiesExample) {
        this.eventType = eventType;
        this.eventTypeName = eventTypeName;
        this.eventPropertiesExample = eventPropertiesExample;
    }
}
