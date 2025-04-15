package com.stratocloud.jpa.entities;

import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageBus;
import com.stratocloud.utils.ContextUtil;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class EntityMessagesHolder {
    private static final ThreadLocal<Map<Long, Queue<Message>>> threadLocal = new ThreadLocal<>();

    private static Queue<Message> ensureQueue(Long entityId){
        if(threadLocal.get() == null)
            threadLocal.set(new HashMap<>());

        var queueMap = threadLocal.get();

        return queueMap.computeIfAbsent(entityId, k->new LinkedList<>());
    }

    public static void offer(Long entityId, Message message){
        Queue<Message> queue = ensureQueue(entityId);
        queue.offer(message);
    }

    public static void flushEntityMessages(Long entityId, boolean doesPublishWithSystemSession){
        Queue<Message> queue = ensureQueue(entityId);
        while (!queue.isEmpty()){
            Message message = queue.poll();
            MessageBus messageBus = ContextUtil.getBean(MessageBus.class);
            if(doesPublishWithSystemSession)
                messageBus.publishWithSystemSession(message);
            else
                messageBus.publish(message);
        }
        eraseQueue(entityId);
    }

    private static void eraseQueue(Long entityId) {
        if(threadLocal.get() == null)
            return;
        var queueMap = threadLocal.get();
        queueMap.remove(entityId);
    }


}
