package com.stratocloud.provider.huawei.obs.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.obs.services.model.*;
import com.stratocloud.form.*;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.utils.Utils;
import lombok.Data;

import java.util.List;

@Data
public class HuaweiBucketUpdateAclInput implements ResourceActionInput {

    @NestedFormField(label = "ACL规则", multiple = true, nestedFormClass = RuleInput.class)
    private List<RuleInput> rules;

    @InputField(label = "存储桶持有者ID", disabled = true, required = false)
    private String ownerId;
    @InputField(label = "存储桶持有者名称", disabled = true, required = false)
    private String ownerName;

    @Data
    public static class RuleInput implements DynamicForm {
        @BooleanField(label = "授权所有用户")
        private boolean grantAllUsers;

        @InputField(label = "账号ID", conditions = "this.grantAllUsers === false")
        private String granteeId;

        @SelectField(
                label = "权限",
                options = {
                        "FULL_CONTROL",
                        "READ",
                        "WRITE",
                        "READ_ACP",
                        "WRITE_ACP",
                        "READ_OBJECT",
                        "FULL_CONTROL_OBJECT"
                },
                optionNames = {
                        "完全控制",
                        "数据读取",
                        "数据写入",
                        "权限读取",
                        "权限写入",
                        "对象读取",
                        "对象完全控制"
                }
        )
        private String permission;

        @JsonIgnore
        public GrantAndPermission toGrant() {
            return new GrantAndPermission(
                    grantAllUsers ? GroupGrantee.ALL_USERS : new CanonicalGrantee(granteeId),
                    Permission.parsePermission(permission)
            );
        }

        public static RuleInput fromGrant(GrantAndPermission grant){
            RuleInput ruleInput = new RuleInput();

            if(grant.getGrantee() instanceof GroupGrantee groupGrantee){
                if(groupGrantee.getGroupGranteeType() == GroupGranteeEnum.ALL_USERS)
                    ruleInput.setGrantAllUsers(true);
            }else if(grant.getGrantee() instanceof CanonicalGrantee canonicalGrantee) {
                ruleInput.setGrantAllUsers(false);
                ruleInput.setGranteeId(canonicalGrantee.getIdentifier());
            }

            ruleInput.setPermission(grant.getPermission().getPermissionString());

            return ruleInput;
        }
    }

    @JsonIgnore
    public AccessControlList toAcl(){
        AccessControlList acl = new AccessControlList();

        if(Utils.isNotBlank(ownerId)){
            Owner owner = new Owner();
            owner.setId(ownerId);
            owner.setDisplayName(ownerName);
            acl.setOwner(owner);
        }


        if(Utils.isNotEmpty(rules)){
            for (RuleInput rule : rules) {
                GrantAndPermission grant = rule.toGrant();
                acl.grantPermission(grant.getGrantee(), grant.getPermission());
            }
        }

        return acl;
    }

    public static HuaweiBucketUpdateAclInput fromAcl(AccessControlList acl){
        HuaweiBucketUpdateAclInput input = new HuaweiBucketUpdateAclInput();

        if(Utils.isNotEmpty(acl.getGrants())){
            input.setRules(
                    acl.getGrants().stream().map(
                            RuleInput::fromGrant
                    ).toList()
            );
        }

        if(acl.getOwner() != null){
            input.setOwnerId(acl.getOwner().getId());
            input.setOwnerName(acl.getOwner().getDisplayName());
        }

        return input;
    }
}
