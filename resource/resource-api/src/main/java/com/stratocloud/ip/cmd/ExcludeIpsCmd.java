package com.stratocloud.ip.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class ExcludeIpsCmd implements ApiCommand {
    private Long ipPoolId;
    private List<String> addresses;
    private String reason;
}
