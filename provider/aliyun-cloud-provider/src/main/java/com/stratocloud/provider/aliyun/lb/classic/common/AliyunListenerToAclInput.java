package com.stratocloud.provider.aliyun.lb.classic.common;

import com.stratocloud.form.SelectField;
import com.stratocloud.provider.relationship.RelationshipConnectInput;
import lombok.Data;

@Data
public class AliyunListenerToAclInput implements RelationshipConnectInput {
    @SelectField(
            label = "访问控制类型",
            options = {"white", "black"},
            optionNames = {"白名单", "黑名单"},
            required = false
    )
    private String aclType;
}
