package com.stratocloud.notification.query;

import com.stratocloud.request.query.NestedAuditable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NestedInternalMail extends NestedAuditable {
    private String eventId;
    private Long receiverUserId;
    private String message;
    private boolean read;
}
