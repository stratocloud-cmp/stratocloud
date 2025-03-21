package com.stratocloud.resource.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class DisassociateTagCmd implements ApiCommand {
    private Long resourceId;
    private String tagKey;
    private String tagValue;
}
