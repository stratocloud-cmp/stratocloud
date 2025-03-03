package com.stratocloud.messaging;

import com.stratocloud.auth.CallContext;
import com.stratocloud.utils.JSON;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Message {
    private String topic;
    private String payload;
    private String uuid;
    private String key;

    @Setter
    private Long triggeredBy;

    public static Message create(String topic, Object payload, String key){
        Message message = new Message();
        message.topic = topic;
        message.payload = JSON.toJsonString(payload);
        message.uuid = UUID.randomUUID().toString();
        message.key = key;

        if(CallContext.exists())
            message.triggeredBy = CallContext.current().getCallingUser().userId();

        return message;
    }

    public static Message create(String topic, Object payload){
        return create(topic, payload, null);
    }
}
