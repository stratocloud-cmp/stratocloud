package com.stratocloud.messaging;

import com.stratocloud.auth.CallContext;

public interface MessageBus {
    void publish(Message message);

    default void publishWithSystemSession(Message message){
        CallContext.registerSystemSession();
        message.setTriggeredBy(CallContext.current().getCallingUser().userId());
        publish(message);
        CallContext.unregister();
    }

    void subscribe(MessageConsumer consumer);
}
