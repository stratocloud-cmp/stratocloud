package com.stratocloud.ip.cmd;

import com.stratocloud.request.ApiCommand;
import com.stratocloud.ip.InternetProtocol;
import lombok.Data;

@Data
public class CreateIpPoolCmd implements ApiCommand {
    private Long tenantId;
    private String name;
    private String description;
    private InternetProtocol protocol;
    private String cidr;
    private String gateway;
}
