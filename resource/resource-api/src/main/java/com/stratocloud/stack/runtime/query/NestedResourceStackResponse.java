package com.stratocloud.stack.runtime.query;

import com.stratocloud.request.query.NestedControllable;
import com.stratocloud.resource.cmd.create.NestedResourceTag;
import com.stratocloud.stack.ResourceStackState;
import com.stratocloud.stack.blueprint.query.NestedBlueprintResponse;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class NestedResourceStackResponse extends NestedControllable {
    private NestedBlueprintResponse blueprint;

    private String name;
    private String description;

    private ResourceStackState state;

    private Boolean recycled;

    private LocalDateTime recycledTime;

    private LocalDateTime expireTime;

    private List<NestedResourceStackNodeResponse> nodes;

    private List<NestedResourceTag> tags;
}
