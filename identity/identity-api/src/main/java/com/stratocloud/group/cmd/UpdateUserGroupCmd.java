package com.stratocloud.group.cmd;

import com.stratocloud.request.JobParameters;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserGroupCmd implements JobParameters {
    private Long userGroupId;
    private String name;
    private String alias;
    private String description;

    private List<NestedUserGroupTag> tags;
}
