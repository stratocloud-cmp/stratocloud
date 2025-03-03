package com.stratocloud.external.resource;

import java.util.Map;

public interface RuleGatewayService {
    String executeNamingRule(String ruleType, Map<String, Object> args);
}
