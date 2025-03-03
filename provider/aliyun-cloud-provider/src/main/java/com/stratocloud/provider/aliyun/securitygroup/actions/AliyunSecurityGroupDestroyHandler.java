package com.stratocloud.provider.aliyun.securitygroup.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.securitygroup.AliyunSecurityGroupHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AliyunSecurityGroupDestroyHandler implements DestroyResourceActionHandler {

    private final AliyunSecurityGroupHandler securityGroupHandler;


    public AliyunSecurityGroupDestroyHandler(AliyunSecurityGroupHandler securityGroupHandler) {
        this.securityGroupHandler = securityGroupHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return securityGroupHandler;
    }

    @Override
    public String getTaskName() {
        return "删除安全组";
    }


    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        if(Utils.isBlank(resource.getExternalId()))
            return;

        AliyunCloudProvider provider = (AliyunCloudProvider) securityGroupHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        provider.buildClient(account).ecs().deleteSecurityGroup(resource.getExternalId());
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        List<Resource> nics = resource.getCapabilitySources(ResourceCategories.NIC);

        if(Utils.isNotEmpty(nics))
            throw new StratoException("Detach all nics from this security group first.");
    }
}
