package com.stratocloud.rule;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageBus;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import java.nio.charset.Charset;

public interface NamingRuleInitializer extends ApplicationContextAware {
    String NAMING_RULE_INITIALIZE_TOPIC = "NAMING_RULE_INITIALIZE";

    String getRuleType();
    String getRuleName();
    String getDefaultScriptPath();
    SuffixPolicy getSuffixPolicy();


    @Override
    default void setApplicationContext(ApplicationContext applicationContext) throws BeansException{
        try {
            MessageBus messageBus = applicationContext.getBean(MessageBus.class);
            Resource resource = applicationContext.getResource(getDefaultScriptPath());
            String script = IOUtils.toString(resource.getURI(), Charset.defaultCharset());
            InitRulePayload payload = new InitRulePayload(getRuleType(), getRuleName(), script, getSuffixPolicy());
            Message message = Message.create(NAMING_RULE_INITIALIZE_TOPIC, payload);
            messageBus.publishWithSystemSession(message);
        }catch (Exception e){
            throw new StratoException(e.getMessage(), e);
        }
    }
}
