package com.stratocloud.rule;

public record InitRulePayload(String ruleType,
                              String ruleName,
                              String script,
                              SuffixPolicy suffixPolicy) {
}
