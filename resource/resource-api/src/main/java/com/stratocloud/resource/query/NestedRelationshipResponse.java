package com.stratocloud.resource.query;

import com.stratocloud.request.query.NestedAuditable;
import com.stratocloud.resource.RelationshipState;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NestedRelationshipResponse extends NestedAuditable {
    private RelationshipState state;
    private NestedResourceResponse source;
    private NestedResourceResponse target;
}
