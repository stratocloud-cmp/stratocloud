package com.stratocloud.stack.runtime.cmd;

import com.stratocloud.request.JobParameters;
import com.stratocloud.resource.cmd.create.NestedResourceTag;
import com.stratocloud.stack.runtime.cmd.nested.CreateResourceStackNodeCmd;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateResourceStacksCmd implements JobParameters {
    private Long tenantId;
    private Long ownerId;

    private Long blueprintId;

    private String name;
    private String description;

    private LocalDateTime expireTime;

    private Integer number = 1;

    private List<CreateResourceStackNodeCmd> nodes;

    private List<NestedResourceTag> tags;
}
