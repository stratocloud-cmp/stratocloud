package com.stratocloud.limit.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class DisableLimitsCmd implements ApiCommand {
    private List<Long> limitIds;
}
