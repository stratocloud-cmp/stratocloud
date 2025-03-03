package com.stratocloud.stack.blueprint.cmd;

import com.stratocloud.request.ApiCommand;
import com.stratocloud.stack.blueprint.cmd.nested.NestedBlueprintNode;
import com.stratocloud.stack.blueprint.cmd.nested.NestedBlueprintRelationship;
import com.stratocloud.utils.Assert;
import lombok.Data;

import java.util.List;

@Data
public class UpdateBlueprintCmd implements ApiCommand {
    private Long blueprintId;

    private String name;
    private String description;

    private List<NestedBlueprintNode> nodes;
    private List<NestedBlueprintRelationship> relationships;

    @Override
    public void validate() {
        Assert.isNotEmpty(nodes);
    }
}
