package com.stratocloud.ip.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class UpdateIpPoolCmd implements ApiCommand {
    private Long ipPoolId;
    private String name;
    private String description;
    private String cidr;
    private String gateway;
}
