package com.stratocloud.group.cmd;

import com.stratocloud.request.JobParameters;
import lombok.Data;

@Data
public class JoinUserGroupCmd implements JobParameters {
    private Long userGroupId;
}
