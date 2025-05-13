package com.stratocloud.notification.internal;

import com.stratocloud.jpa.entities.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InternalMail extends Auditable {
    @Column(nullable = false)
    private String eventId;
    @Column(nullable = false)
    private Long receiverUserId;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    @Column(nullable = false)
    private boolean read = false;

    public InternalMail(String eventId, Long receiverUserId, String message) {
        this.eventId = eventId;
        this.receiverUserId = receiverUserId;
        this.message = message;
    }

    public void markRead() {
        this.read = true;
    }
}
