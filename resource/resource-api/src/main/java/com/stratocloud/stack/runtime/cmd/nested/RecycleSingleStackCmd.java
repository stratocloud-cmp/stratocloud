package com.stratocloud.stack.runtime.cmd.nested;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class RecycleSingleStackCmd implements ApiCommand {
    private Long stackId;
    private Boolean executingDestruction = true;
}
