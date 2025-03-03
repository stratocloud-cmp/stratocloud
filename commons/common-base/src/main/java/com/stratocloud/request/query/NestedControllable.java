package com.stratocloud.request.query;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class NestedControllable extends NestedTenanted {
    private Long ownerId;

    private String ownerLoginName;
    private String ownerRealName;
}
