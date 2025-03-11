package com.stratocloud.rule;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageConsumer;
import com.stratocloud.repository.RuleRepository;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class InitNamingRulesConsumer implements MessageConsumer {

    private final RuleRepository ruleRepository;

    public InitNamingRulesConsumer(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Override
    @Transactional
    public void consume(Message message) {
        InitRulePayload payload = JSON.toJavaObject(message.getPayload(), InitRulePayload.class);

        String ruleType = payload.ruleType();

        Optional<Rule> rule = ruleRepository.findRuleByType(ruleType);

        if(rule.isEmpty()) {
            createRule(payload);
            return;
        }

        if(rule.get() instanceof NamingRule namingRule){
            namingRule.update(payload.ruleName(), payload.script());
            namingRule.updateSuffixPolicy(payload.suffixPolicy());
            ruleRepository.save(namingRule);
        }else {
            throw new StratoException("Rule type %s conflicts. It is not a naming rule.".formatted(ruleType));
        }
    }

    private void createRule(InitRulePayload payload) {
        NamingRule rule = new NamingRule();
        rule.setType(payload.ruleType());
        rule.setName(payload.ruleName());
        rule.setScript(payload.script());

        rule.setSuffixType(payload.suffixPolicy().type());
        rule.setSuffixLength(payload.suffixPolicy().suffixLength());
        rule.setSuffixStartNumber(payload.suffixPolicy().suffixStartNumber());

        rule.setBuiltIn(true);

        ruleRepository.save(rule);
    }

    @Override
    public String getTopic() {
        return NamingRuleInitializer.NAMING_RULE_INITIALIZE_TOPIC;
    }

    @Override
    public String getConsumerGroup() {
        return "RULE";
    }
}
