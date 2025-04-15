package com.stratocloud.messaging;

import com.stratocloud.auth.CallContext;

public interface MessageBus {
    void publish(Message message);

    default void publishWithSystemSession(Message message){
        CallContext current = null;

        if(CallContext.exists())
            current = CallContext.current();

        CallContext.registerSystemSession();
        message.setTriggeredBy(CallContext.current().getCallingUser().userId());
        publish(message);

        if(current != null)
            CallContext.registerBack(current);
        else
            CallContext.unregister();
    }

    void subscribe(MessageConsumer consumer);
}
