package com.stratocloud.order.event;

import java.util.Map;

public record OrderItemProperties(String nodeType,
                                  String nodeKey,
                                  String nodeName,
                                  String jobType,
                                  String jobTypeName,
                                  Map<String, Object> parameters) {
}
