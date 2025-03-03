package com.stratocloud.provider.huawei.nic.requirements;

import com.huaweicloud.sdk.vpc.v2.model.Port;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.nic.HuaweiNicHandler;
import com.stratocloud.provider.huawei.nic.HuaweiNicHelper;
import com.stratocloud.provider.huawei.servers.HuaweiServerHandler;
import com.stratocloud.provider.relationship.EssentialPrimaryCapabilityHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiPrimaryNicToServerHandler
        implements EssentialRequirementHandler, EssentialPrimaryCapabilityHandler {

    private final HuaweiNicHandler nicHandler;

    private final HuaweiServerHandler serverHandler;

    public HuaweiPrimaryNicToServerHandler(HuaweiNicHandler nicHandler,
                                           HuaweiServerHandler serverHandler) {
        this.nicHandler = nicHandler;
        this.serverHandler = serverHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_PRIMARY_NIC_TO_SERVER_HANDLER";
    }

    @Override
    public String getRelationshipTypeName() {
        return "云主机与主网卡";
    }

    @Override
    public ResourceHandler getSource() {
        return nicHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return serverHandler;
    }

    @Override
    public String getCapabilityName() {
        return "主网卡";
    }

    @Override
    public String getRequirementName() {
        return "云主机(主网卡)";
    }

    @Override
    public String getConnectActionName() {
        return "挂载主网卡";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除挂载";
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<Port> port = nicHandler.describePort(account, source.externalId());

        if(port.isEmpty())
            return List.of();

        if(!HuaweiNicHelper.isPrimaryInterface(port.get()))
            return List.of();

        String deviceId = port.get().getDeviceId();

        Optional<ExternalResource> server = serverHandler.describeExternalResource(account, deviceId);

        return server.map(s -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        s,
                        Map.of()
                )
        )).orElseGet(List::of);

    }
}
