package com.stratocloud.provider.tencent.cos.bucket.actions;

import com.qcloud.cos.model.BucketRefererConfiguration;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.cos.session.CosSession;
import com.stratocloud.provider.tencent.cos.session.CosSessionKey;
import com.stratocloud.provider.tencent.cos.session.CosSessionManager;
import com.stratocloud.resource.Resource;
import lombok.Data;

import java.util.List;

@Data
public class TencentBucketUpdateRefererInput implements ResourceActionInput {
    @SelectField(
            label = "当前状态",
            defaultValues = "Enabled",
            options = {
                    "Enabled",
                    "Disabled"
            },
            optionNames = {
                    "开启",
                    "不开启"
            }
    )
    private String status;

    @SelectField(
            label = "防盗链类型",
            options = {
                    "Black-List",
                    "White-List"
            },
            optionNames = {
                    "黑名单",
                    "白名单"
            },
            defaultValues = "White-List"
    )
    private String refererType;

    @SelectField(
            label = "Referer",
            multiSelect = true,
            allowCreate = true
    )
    private List<String> domainList;

    @SelectField(
            label = "空Referer访问",
            options = {
                    "Allow",
                    "Deny"
            },
            optionNames = {
                    "允许",
                    "拒绝"
            },
            defaultValues = "Deny"
    )
    private String emptyReferer;

    public static TencentBucketUpdateRefererInput getInput(Resource bucketResource){
        var provider = (TencentCloudProvider) bucketResource.getResourceHandler().getProvider();
        var account = provider.getAccountRepository().findExternalAccount(bucketResource.getAccountId());
        CosSessionKey sessionKey = provider.buildClient(account).getCosSessionKey();
        CosSession cosSession = CosSessionManager.getSession(sessionKey);
        BucketRefererConfiguration referer = cosSession.describeBucketReferer(
                bucketResource.getExternalId()
        ).orElseThrow(
                () -> new StratoException("Referer config not found")
        );

        TencentBucketUpdateRefererInput t = new TencentBucketUpdateRefererInput();
        t.setStatus(referer.getStatus());
        t.setRefererType(referer.getRefererType());
        t.setDomainList(referer.getDomainList());
        t.setEmptyReferer(referer.getEmptyReferConfiguration());
        return t;
    }
}
