package com.stratocloud.group.cmd;

import com.stratocloud.request.JobParameters;
import lombok.Data;

import java.util.List;

@Data
public class AddUsersToGroupCmd implements JobParameters {
    private Long userGroupId;
    private List<Long> userIds;
}
