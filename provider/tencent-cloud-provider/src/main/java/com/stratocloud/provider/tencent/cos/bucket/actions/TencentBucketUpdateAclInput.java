package com.stratocloud.provider.tencent.cos.bucket.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qcloud.cos.model.*;
import com.stratocloud.form.*;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.utils.Utils;
import lombok.Data;

import java.util.List;

@Data
public class TencentBucketUpdateAclInput implements ResourceActionInput {

    @NestedFormField(label = "ACL规则", multiple = true, nestedFormClass = RuleInput.class)
    private List<RuleInput> rules;

    @Data
    public static class RuleInput implements DynamicForm {
        @BooleanField(label = "授权所有用户")
        private boolean grantAllUsers;

        @InputField(label = "账号ID", conditions = "this.grantAllUsers === false")
        private String granteeId;

        @SelectField(
                label = "权限",
                options = {
                        "FullControl",
                        "Read",
                        "Write",
                        "ReadAcp",
                        "WriteAcp"
                },
                optionNames = {
                        "完全控制",
                        "数据读取",
                        "数据写入",
                        "权限读取",
                        "权限写入"
                }
        )
        private Permission permission;

        @JsonIgnore
        public Grant toGrant() {
            return new Grant(
                    grantAllUsers ? GroupGrantee.AllUsers : new UinGrantee(granteeId),
                    permission
            );
        }

        public static RuleInput fromGrant(Grant grant){
            RuleInput ruleInput = new RuleInput();

            if(grant.getGrantee() == GroupGrantee.AllUsers){
                ruleInput.setGrantAllUsers(true);
            }else if(grant.getGrantee() instanceof UinGrantee uinGrantee) {
                ruleInput.setGrantAllUsers(false);
                ruleInput.setGranteeId(uinGrantee.getIdentifier());
            }

            ruleInput.setPermission(grant.getPermission());

            return ruleInput;
        }
    }

    @JsonIgnore
    public AccessControlList toAcl(){
        AccessControlList acl = new AccessControlList();

        if(Utils.isNotEmpty(rules)){
            for (RuleInput rule : rules) {
                Grant grant = rule.toGrant();
                acl.grantPermission(grant.getGrantee(), grant.getPermission());
            }
        }

        return acl;
    }

    public static TencentBucketUpdateAclInput fromAcl(AccessControlList acl){
        TencentBucketUpdateAclInput input = new TencentBucketUpdateAclInput();

        if(Utils.isNotEmpty(acl.getGrantsAsList())){
            input.setRules(
                    acl.getGrantsAsList().stream().map(
                            RuleInput::fromGrant
                    ).toList()
            );
        }

        return input;
    }
}
