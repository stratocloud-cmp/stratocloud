package com.stratocloud.order.query;

import com.stratocloud.request.query.NestedAuditable;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;


@Getter
@Setter
public class NestedOrderItem extends NestedAuditable {
    private Long jobId;
    private String jobType;
    private String jobTypeName;
    private String nodeName;
    private Map<String, Object> parameters;
}
