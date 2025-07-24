package com.stratocloud.provider.tencent.cos.acl.actions;

import com.qcloud.cos.model.AccessControlList;
import com.qcloud.cos.model.Grant;
import com.qcloud.cos.model.Permission;
import com.qcloud.cos.model.UinGrantee;
import com.stratocloud.utils.Utils;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
public class TencentBucketAclSpec {
    private String granteeId;

    private boolean allowFullControl;
    private boolean allowRead;
    private boolean allowWrite;
    private boolean allowReadAcp;
    private boolean allowWriteAcp;

    public AccessControlList apply(AccessControlList acl) {
        List<Grant> currentGrants = acl.getGrantsAsList();

        List<Grant> newGrants = new ArrayList<>();

        if(Utils.isNotEmpty(currentGrants)){
            List<Grant> grantsToKeep = currentGrants.stream().filter(
                    g -> !Objects.equals(g.getGrantee().getIdentifier(), granteeId)
            ).toList();
            newGrants.addAll(grantsToKeep);
        }

        if(allowFullControl){
            newGrants.add(
                    new Grant(
                            new UinGrantee(granteeId),
                            Permission.FullControl
                    )
            );
        } else {
            if(allowRead){
                newGrants.add(
                        new Grant(
                                new UinGrantee(granteeId),
                                Permission.Read
                        )
                );
            }
            if(allowWrite){
                newGrants.add(
                        new Grant(
                                new UinGrantee(granteeId),
                                Permission.Write
                        )
                );
            }
            if(allowReadAcp){
                newGrants.add(
                        new Grant(
                                new UinGrantee(granteeId),
                                Permission.ReadAcp
                        )
                );
            }
            if(allowWriteAcp){
                newGrants.add(
                        new Grant(
                                new UinGrantee(granteeId),
                                Permission.WriteAcp
                        )
                );
            }
        }

        AccessControlList result = new AccessControlList();
        result.setOwner(acl.getOwner());

        for (Grant newGrant : newGrants)
            result.grantPermission(newGrant.getGrantee(), newGrant.getPermission());

        return result;
    }
}
