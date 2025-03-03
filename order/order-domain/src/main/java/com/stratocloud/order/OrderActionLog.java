package com.stratocloud.order;

import com.stratocloud.auth.CallContext;
import com.stratocloud.auth.UserSession;
import com.stratocloud.jpa.entities.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

@Entity
@Getter
public class OrderActionLog extends Auditable {
    @ManyToOne
    private Order order;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private Long handlerId;
    @Column(nullable = false)
    private String handlerName;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "TEXT")
    private String message;

    public static OrderActionLog of(Order order, String action, String description, String message){
        OrderActionLog log = new OrderActionLog();
        log.order = order;
        log.action = action;
        log.description = description;
        log.message = message;
        UserSession callingUser = CallContext.current().getCallingUser();
        log.handlerId = callingUser.userId();
        log.handlerName = callingUser.realName();
        return log;
    }
}
