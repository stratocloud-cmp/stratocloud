package com.stratocloud.notification.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class MarkInternalMailsReadCmd implements ApiCommand {
    private List<Long> internalMailIds;
}
