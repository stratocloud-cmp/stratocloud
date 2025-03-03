package com.stratocloud.messaging;

public interface MessageConsumer {

    void consume(Message message);

    String getTopic();

    String getConsumerGroup();
}
