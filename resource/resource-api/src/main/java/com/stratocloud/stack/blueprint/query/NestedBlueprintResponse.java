package com.stratocloud.stack.blueprint.query;

import com.stratocloud.request.query.NestedTenanted;
import com.stratocloud.stack.blueprint.cmd.nested.NestedBlueprintNode;
import com.stratocloud.stack.blueprint.cmd.nested.NestedBlueprintRelationship;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NestedBlueprintResponse extends NestedTenanted {
    private String name;
    private String description;

    private List<NestedBlueprintNode> nodes;
    private List<NestedBlueprintRelationship> relationships;
}
