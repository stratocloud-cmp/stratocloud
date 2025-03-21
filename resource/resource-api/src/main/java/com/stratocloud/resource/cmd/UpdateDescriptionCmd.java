package com.stratocloud.resource.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class UpdateDescriptionCmd implements ApiCommand {
    private Long resourceId;
    private String description;
}
