package com.stratocloud.provider.tencent.cos.acl.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class TencentBucketAclUpdateInput implements ResourceActionInput {
    @BooleanField(label = "完全控制")
    private boolean allowFullControl;

    @BooleanField(label = "数据读取", conditions = "this.allowFullControl === false", defaultValue = true)
    private boolean allowRead;

    @BooleanField(label = "数据写入", conditions = "this.allowFullControl === false", defaultValue = true)
    private boolean allowWrite;

    @BooleanField(label = "权限读取", conditions = "this.allowFullControl === false")
    private boolean allowReadAcp;

    @BooleanField(label = "权限写入", conditions = "this.allowFullControl === false")
    private boolean allowWriteAcp;

    public TencentBucketAclSpec toSpec(String granteeId){
        TencentBucketAclSpec spec = new TencentBucketAclSpec();
        spec.setGranteeId(granteeId);
        spec.setAllowFullControl(allowFullControl);
        spec.setAllowRead(allowRead);
        spec.setAllowWrite(allowWrite);
        spec.setAllowReadAcp(allowReadAcp);
        spec.setAllowWriteAcp(allowWriteAcp);
        return spec;
    }
}
