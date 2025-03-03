package com.stratocloud.provider.tencent.nic.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.ip.IpAllocator;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.nic.TencentNicHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import com.tencentcloudapi.vpc.v20170312.models.NetworkInterface;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class TencentNicDestroyHandler implements DestroyResourceActionHandler {
    private final TencentNicHandler nicHandler;
    private final IpAllocator ipAllocator;

    public TencentNicDestroyHandler(TencentNicHandler nicHandler,
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

        Optional<NetworkInterface> nic = nicHandler.describeNic(account, resource.getExternalId());

        if(nic.isEmpty())
            return;

        Resource subnet = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not found when deleting nic.")
        );

        if(!resource.isPrimaryTo(ResourceCategories.COMPUTE_INSTANCE)){
            TencentCloudProvider provider = (TencentCloudProvider) nicHandler.getProvider();
            provider.buildClient(account).deleteNic(resource.getExternalId());
        }

        ipAllocator.releaseIps(subnet, InternetProtocol.IPv4, nicHandler.getIps(nic.get().getPrivateIpAddressSet()));
        ipAllocator.releaseIps(subnet, InternetProtocol.IPv6, nicHandler.getIps(nic.get().getIpv6AddressSet()));
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
