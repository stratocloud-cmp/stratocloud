package com.stratocloud.provider.huawei.elb.member.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.elb.member.HuaweiLbPoolMemberHandler;
import com.stratocloud.provider.huawei.elb.member.HuaweiMember;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiLbPoolMemberDestroyHandler implements DestroyResourceActionHandler {

    private final HuaweiLbPoolMemberHandler memberHandler;

    public HuaweiLbPoolMemberDestroyHandler(HuaweiLbPoolMemberHandler memberHandler) {
        this.memberHandler = memberHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return memberHandler;
    }

    @Override
    public String getTaskName() {
        return "移除后端服务器组成员";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<HuaweiMember> member = memberHandler.describeMember(account, resource.getExternalId());

        if(member.isEmpty())
            return;

        HuaweiCloudProvider provider = (HuaweiCloudProvider) memberHandler.getProvider();
        provider.buildClient(account).elb().deleteLbPoolMember(member.get().memberId());
    }
}
