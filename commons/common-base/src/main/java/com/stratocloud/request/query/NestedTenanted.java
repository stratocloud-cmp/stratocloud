package com.stratocloud.request.query;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class NestedTenanted extends NestedAuditable {
    private Long tenantId;
}
