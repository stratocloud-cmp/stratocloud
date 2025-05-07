package com.stratocloud.notification;

import com.stratocloud.jpa.entities.Tenanted;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Slf4j
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends Tenanted {
    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String, Object> eventProperties;
    @ManyToOne
    private NotificationPolicy policy;
    @ManyToOne
    private NotificationWay notificationWay;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NotificationReceiver> receivers;
}
