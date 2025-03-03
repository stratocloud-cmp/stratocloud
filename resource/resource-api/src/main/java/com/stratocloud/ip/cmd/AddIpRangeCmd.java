package com.stratocloud.ip.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class AddIpRangeCmd implements ApiCommand {
    private Long ipPoolId;
    private String startIp;
    private String endIp;
}
