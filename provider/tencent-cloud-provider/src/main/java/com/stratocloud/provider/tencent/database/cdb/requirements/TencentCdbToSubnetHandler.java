package com.stratocloud.provider.tencent.database.cdb.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.database.cdb.TencentCdbHandler;
import com.stratocloud.provider.tencent.subnet.TencentSubnetHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.tencentcloudapi.cdb.v20170320.models.InstanceInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentCdbToSubnetHandler implements EssentialRequirementHandler {

    private final TencentCdbHandler cdbHandler;

    private final TencentSubnetHandler subnetHandler;

    public TencentCdbToSubnetHandler(TencentCdbHandler cdbHandler, TencentSubnetHandler subnetHandler) {
        this.cdbHandler = cdbHandler;
        this.subnetHandler = subnetHandler;
    }


    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_CDB_TO_SUBNET_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "云数据库与子网";
    }

    @Override
    public ResourceHandler getSource() {
        return cdbHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return subnetHandler;
    }

    @Override
    public String getCapabilityName() {
        return "云数据库";
    }

    @Override
    public String getRequirementName() {
        return "子网";
    }

    @Override
    public String getConnectActionName() {
        return "关联";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除关联";
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<InstanceInfo> instanceInfo = cdbHandler.describeCdb(account, source.externalId());

        if(instanceInfo.isEmpty())
            return List.of();

        Optional<ExternalResource> subnet = subnetHandler.describeExternalResource(
                account, instanceInfo.get().getUniqSubnetId()
        );

        return subnet.map(
                r -> List.of(
                        new ExternalRequirement(
                                getRelationshipTypeId(),
                                r,
                                Map.of()
                        )
                )
        ).orElseGet(List::of);
    }
}
