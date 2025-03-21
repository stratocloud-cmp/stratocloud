package com.stratocloud.resource.cmd;

import com.stratocloud.request.ApiCommand;
import com.stratocloud.resource.cmd.create.NestedResourceTag;
import lombok.Data;

import java.util.List;

@Data
public class AssociateTagsCmd implements ApiCommand {
    private Long resourceId;
    private List<NestedResourceTag> tags;
}
