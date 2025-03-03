package com.stratocloud.role.query;

import com.stratocloud.request.query.NestedAuditable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NestedPermission extends NestedAuditable {
    private String target;
    private String targetName;
    private String action;
    private String actionName;
}
