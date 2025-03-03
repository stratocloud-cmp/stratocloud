package com.stratocloud.stack.runtime.cmd;

import com.stratocloud.request.ApiCommand;
import com.stratocloud.resource.cmd.create.NestedResourceTag;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdateStackBasicsCmd implements ApiCommand {
    private Long stackId;
    private String name;
    private String description;

    private LocalDateTime expireTime;

    private List<NestedResourceTag> tags;
}
