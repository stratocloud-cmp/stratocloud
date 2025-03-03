package com.stratocloud.ip;

import com.stratocloud.ip.query.NestedAttachedNetworkResource;
import com.stratocloud.ip.query.NestedIpPoolResponse;
import com.stratocloud.ip.query.NestedIpRange;
import com.stratocloud.ip.query.NestedIpResponse;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.query.metadata.NestedRelationshipSpec;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class IpPoolAssembler {
    public NestedIpPoolResponse toNestedIpPoolResponse(IpPool ipPool) {
        NestedIpPoolResponse nestedIpPoolResponse = new NestedIpPoolResponse();

        EntityUtil.copyBasicFields(ipPool, nestedIpPoolResponse);

        nestedIpPoolResponse.setName(ipPool.getName());
        nestedIpPoolResponse.setDescription(ipPool.getDescription());
        nestedIpPoolResponse.setProtocol(ipPool.getProtocol());
        nestedIpPoolResponse.setCidr(ipPool.getCidr().value());
        nestedIpPoolResponse.setGateway(ipPool.getGateway().address());
        nestedIpPoolResponse.setRanges(
                toNestedIpRanges(ipPool.getRanges())
        );
        nestedIpPoolResponse.setAttachedNetworkResources(
                toNestedAttachedNetworkResources(ipPool.getAttachedNetworkResources())
        );
        return nestedIpPoolResponse;

    }

    private List<NestedAttachedNetworkResource> toNestedAttachedNetworkResources(List<Resource> attachedNetworkResources) {
        List<NestedAttachedNetworkResource> result = new ArrayList<>();

        if(Utils.isEmpty(attachedNetworkResources)){
            return result;
        }

        for (Resource resource : attachedNetworkResources) {
            result.add(toNestedAttachedNetworkResource(resource));
        }

        return result;
    }

    private NestedAttachedNetworkResource toNestedAttachedNetworkResource(Resource resource) {
        NestedAttachedNetworkResource nestedAttachedNetworkResource = new NestedAttachedNetworkResource();
        nestedAttachedNetworkResource.setResourceId(resource.getId());
        nestedAttachedNetworkResource.setResourceName(resource.getName());
        return nestedAttachedNetworkResource;
    }

    private List<NestedIpRange> toNestedIpRanges(List<IpRange> ranges) {
        List<NestedIpRange> result = new ArrayList<>();

        if(Utils.isEmpty(ranges))
            return result;

        for (IpRange range : ranges) {
            result.add(toNestedIpRange(range));
        }
        return result;
    }

    private NestedIpRange toNestedIpRange(IpRange range) {
        NestedIpRange nestedIpRange = new NestedIpRange();
        EntityUtil.copyBasicFields(range, nestedIpRange);
        nestedIpRange.setStartIp(String.valueOf(range.getStartIp()));
        nestedIpRange.setEndIp(String.valueOf(range.getEndIp()));
        return nestedIpRange;
    }


    public NestedIpResponse toNestedIpResponse(ManagedIp managedIp) {
        NestedIpResponse response = new NestedIpResponse();

        EntityUtil.copyBasicFields(managedIp, response);

        response.setRangeId(managedIp.getRange().getId());
        response.setIpPoolId(managedIp.getIpPoolId());

        response.setAddress(managedIp.getAddress().address());
        response.setToBigInteger(managedIp.getToBigInteger().toString());
        response.setState(managedIp.getState());

        response.setResourceId(managedIp.getResourceId());
        response.setResourceName(managedIp.getResourceName());
        response.setResourceCategory(managedIp.getResourceCategory());

        response.setAllocationReason(managedIp.getAllocationReason());

        return response;
    }
}
