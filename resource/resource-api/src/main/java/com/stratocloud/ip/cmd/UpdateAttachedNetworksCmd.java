package com.stratocloud.ip.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class UpdateAttachedNetworksCmd implements ApiCommand {
    private Long ipPoolId;
    private List<Long> attachedNetworkResourceIds;
}
