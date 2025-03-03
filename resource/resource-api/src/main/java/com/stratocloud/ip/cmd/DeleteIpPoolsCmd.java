package com.stratocloud.ip.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class DeleteIpPoolsCmd implements ApiCommand {
    private List<Long> ipPoolIds;
}
