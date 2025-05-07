package com.stratocloud.notification;

import com.stratocloud.jpa.entities.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(uniqueConstraints = @UniqueConstraint(name = "unique_idx_event_type", columnNames = "event_type"))
public class NotificationEventType extends Auditable {
    @Column(nullable = false)
    private String eventType;
    @Column(nullable = false)
    private String eventTypeName;
}
