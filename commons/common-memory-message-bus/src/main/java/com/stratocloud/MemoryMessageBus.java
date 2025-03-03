package com.stratocloud;

import com.stratocloud.auth.CallContext;
import com.stratocloud.auth.UserSession;
import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageBus;
import com.stratocloud.messaging.MessageConsumer;
import com.stratocloud.utils.ContextUtil;
import com.stratocloud.config.MonolithOnly;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Component
@MonolithOnly
public class MemoryMessageBus implements MessageBus {
    private final Map<String, Queue<QueuedMessage>> queueMap = new ConcurrentHashMap<>();

    private final Map<String, ConsumerGroups> consumerGroupsMap = new ConcurrentHashMap<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(15);


    @Override
    public void publish(Message message) {
        Queue<QueuedMessage> queue = queueMap.computeIfAbsent(
                message.getTopic(), k -> new LinkedBlockingQueue<>()
        );

        queue.offer(new QueuedMessage(message, CallContext.current().getCallingUser()));

        log.debug("Message published. Topic={}. UUID={}.", message.getTopic(), message.getUuid());
    }


    @Override
    public synchronized void subscribe(MessageConsumer consumer) {
        queueMap.computeIfAbsent(consumer.getTopic(), k->new LinkedBlockingQueue<>());

        ConsumerGroups consumerGroups = consumerGroupsMap.computeIfAbsent(
                consumer.getTopic(),
                k -> new ConsumerGroups(k, new HashSet<>())
        );

        consumerGroups.consumerClasses().add(consumer.getClass());

        log.debug("Message consumer registered. Topic={}. ConsumerGroup={}.",
                consumer.getTopic(), consumer.getConsumerGroup()
        );
    }

    @Scheduled(fixedDelay = 2000L)
    public void consumeMessages(){
        for (ConsumerGroups consumerGroups : consumerGroupsMap.values()) {
            Queue<QueuedMessage> queue = queueMap.get(consumerGroups.topic());

            if(queue == null) {
                continue;
            }

            ApplicationContext applicationContext = ContextUtil.getApplicationContext();

            if(applicationContext == null){
                log.warn("ApplicationContext not ready, retrying later...");
                return;
            }

            for (Class<? extends MessageConsumer> consumerClass : consumerGroups.consumerClasses()) {
                Optional<MessageConsumer> consumer = getMessageConsumer(
                        consumerGroups, applicationContext, consumerClass
                );

                if (consumer.isEmpty())
                    continue;

                while (!queue.isEmpty()){
                    QueuedMessage queuedMessage = queue.poll();
                    MessageConsumingTask task = new MessageConsumingTask(queuedMessage, consumer.get());
                    executorService.submit(task);
                    log.debug("Message consuming task submitted.");
                }
            }
        }
    }

    private static Optional<MessageConsumer> getMessageConsumer(ConsumerGroups consumerGroups,
                                                                ApplicationContext applicationContext,
                                                                Class<? extends MessageConsumer> consumerClass) {
        MessageConsumer consumer;
        try {
            consumer = applicationContext.getBean(consumerClass);
        }catch (NoSuchBeanDefinitionException e){
            try {
                consumer = applicationContext.getBean(consumerGroups.topic(), consumerClass);
            }catch (NoSuchBeanDefinitionException e2){
                log.warn("Consumer {} not ready, retrying later...", consumerClass.getSimpleName());
                return Optional.empty();
            }
        }catch (Exception e){
            log.error(e.getMessage(), e);
            return Optional.empty();
        }
        return Optional.of(consumer);
    }

    private record QueuedMessage(Message message, UserSession userSession){

    }

    private record ConsumerGroups(String topic, Set<Class<? extends MessageConsumer>> consumerClasses){

    }

    private record MessageConsumingTask(QueuedMessage queuedMessage,
                                        MessageConsumer consumer) implements Runnable{

        @Override
        public void run() {
            UserSession userSession = queuedMessage.userSession();
            Message message = queuedMessage.message();
            try{
                CallContext.register(userSession);
                log.debug("Message received by consumer group {}. Topic={}. UUID={}.",
                        consumer.getConsumerGroup(), message.getTopic(), message.getUuid());
                consumer.consume(message);
                log.debug("Message consumed successfully. Topic={}. UUID={}.", message.getTopic(), message.getUuid());
            }catch (Exception e){
                log.error("Failed to consume message. Topic={}. UUID={}. Payload={}.",
                        message.getTopic(), message.getUuid(), message.getPayload(), e);
            }finally {
                CallContext.unregister();
            }
        }
    }
}
