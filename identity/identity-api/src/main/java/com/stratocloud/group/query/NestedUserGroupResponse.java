package com.stratocloud.group.query;

import com.stratocloud.group.cmd.NestedUserGroupTag;
import com.stratocloud.request.query.NestedTenanted;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NestedUserGroupResponse extends NestedTenanted {
    private String name;
    private String alias;
    private String description;
    private List<NestedUserGroupTag> tags;
}
