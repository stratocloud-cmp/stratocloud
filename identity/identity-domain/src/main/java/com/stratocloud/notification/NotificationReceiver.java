package com.stratocloud.notification;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.jpa.entities.Auditable;
import com.stratocloud.utils.Utils;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationReceiver extends Auditable {
    @ManyToOne
    private Notification notification;

    @Column(nullable = false)
    private Long receiverUserId;
    @Column(nullable = false)
    private String receiverUserRealName;
    @Column(nullable = false)
    private int successfullySentCount = 0;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationSendState state = NotificationSendState.NO_STATE;
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    public NotificationReceiver(Notification notification,
                                Long receiverUserId,
                                String receiverUserRealName) {
        this.notification = notification;
        this.receiverUserId = receiverUserId;
        this.receiverUserRealName = receiverUserRealName;
    }

    public void send(){
        NotificationWay notificationWay = notification.getPolicy().getNotificationWay();

        try {
            notificationWay.getProvider().sendNotification(this);
            successfullySentCount++;
            this.state = NotificationSendState.SUCCESS;
        }catch (Exception e){
            this.state = NotificationSendState.FAILED;
            this.errorMessage = e.getMessage();
        }
    }


    public String getRenderedHtmlMessage(String stratoDomainName) {
        Map<String, Object> inputParameters = new HashMap<>();

        inputParameters.put("receiverId", receiverUserId);
        inputParameters.put("receiverName", receiverUserRealName);
        inputParameters.put("domainName", stratoDomainName);
        inputParameters.put("eventSummary", notification.getEventSummary());
        inputParameters.put("eventHappenedAt", notification.getEventHappenedAt());

        if(Utils.isNotEmpty(notification.getEventProperties()))
            inputParameters.putAll(notification.getEventProperties());

        Velocity.init();
        VelocityContext velocityContext = new VelocityContext(inputParameters);

        StringWriter stringWriter = new StringWriter();

        boolean evaluated = Velocity.evaluate(
                velocityContext,
                stringWriter,
                notification.getPolicy().getPolicyKey(),
                notification.getPolicy().getTemplate()
        );

        if(!evaluated)
            throw new StratoException("Velocity evaluation failed");

        return stringWriter.toString();
    }
}
