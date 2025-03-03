package com.stratocloud.external.order;

import java.util.Map;

public interface RuleGatewayService {
    String executeNamingRule(String ruleType, Map<String, Object> args);
}
