package com.stratocloud.provider.aliyun.nic.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.ip.IpAllocator;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.nic.AliyunNic;
import com.stratocloud.provider.aliyun.nic.AliyunNicHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class AliyunNicDestroyHandler implements DestroyResourceActionHandler {
    private final AliyunNicHandler nicHandler;
    private final IpAllocator ipAllocator;

    public AliyunNicDestroyHandler(AliyunNicHandler nicHandler,
                                   IpAllocator ipAllocator) {
        this.nicHandler = nicHandler;
        this.ipAllocator = ipAllocator;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return nicHandler;
    }

    @Override
    public String getTaskName() {
        return "删除弹性网卡";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<AliyunNic> nic = nicHandler.describeNic(account, resource.getExternalId());

        if(nic.isEmpty())
            return;

        Resource subnet = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not found when deleting nic.")
        );

        if(!resource.isPrimaryTo(ResourceCategories.COMPUTE_INSTANCE)){
            AliyunCloudProvider provider = (AliyunCloudProvider) nicHandler.getProvider();
            provider.buildClient(account).ecs().deleteNic(resource.getExternalId());
        }

        ipAllocator.releaseIps(subnet, InternetProtocol.IPv4, nic.get().getPrivateIps());
        ipAllocator.releaseIps(subnet, InternetProtocol.IPv6, nic.get().getIpv6List());
    }


    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        if(resource.isPrimaryTo(ResourceCategories.COMPUTE_INSTANCE)) {
            resource.onDestroyed();
            return ResourceActionResult.finished();
        }

        return DestroyResourceActionHandler.super.checkActionResult(resource, parameters);
    }
}
