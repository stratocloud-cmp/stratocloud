package com.stratocloud.group.cmd;

import com.stratocloud.request.JobParameters;
import lombok.Data;

import java.util.List;

@Data
public class DeleteUserGroupsCmd implements JobParameters {
    private List<Long> userGroupIds;
}
