package com.stratocloud.ip.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class RemoveIpRangesCmd implements ApiCommand {
    private Long ipPoolId;
    private List<Long> ipRangeIds;
}
