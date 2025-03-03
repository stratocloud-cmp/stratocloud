package com.stratocloud.tenant;

import com.stratocloud.identity.SimpleTenant;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.tenant.query.DescribeSimpleTenantsResponse;
import com.stratocloud.tenant.query.DescribeTenantsTreeResponse;
import com.stratocloud.tenant.query.NestedTenantResponse;
import com.stratocloud.tenant.query.NestedTenantsTreeNode;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TenantAssembler {
    public DescribeSimpleTenantsResponse toSimpleTenantsResponse(List<Tenant> inheritedTenants) {
        List<SimpleTenant> simpleTenants = new ArrayList<>();

        if(Utils.isEmpty(inheritedTenants))
            return new DescribeSimpleTenantsResponse(simpleTenants);

        for (Tenant inheritedTenant : inheritedTenants) {
            SimpleTenant simpleTenant = new SimpleTenant(inheritedTenant.getId(), inheritedTenant.getName());
            simpleTenants.add(simpleTenant);
        }

        return new DescribeSimpleTenantsResponse(simpleTenants);
    }

    public NestedTenantResponse toNestedTenantResponse(Tenant tenant) {
        NestedTenantResponse response = new NestedTenantResponse();

        EntityUtil.copyBasicFields(tenant, response);

        response.setName(tenant.getName());
        response.setDescription(tenant.getDescription());
        response.setDisabled(tenant.getDisabled());

        if(tenant.getParent()!=null){
            response.setParentId(tenant.getParent().getId());
            response.setParentName(tenant.getParent().getName());
        }


        return response;
    }

    public DescribeTenantsTreeResponse toTenantsTreeResponse(List<Tenant> roots) {

        Queue<Tenant> queue = new LinkedList<>();
        roots.forEach(queue::offer);
        Map<Tenant, NestedTenantsTreeNode> map = new HashMap<>();
        while (!queue.isEmpty()){
            Tenant tenant = queue.poll();
            NestedTenantsTreeNode currentNode = new NestedTenantsTreeNode(toNestedTenantResponse(tenant));
            NestedTenantsTreeNode parentNode = map.get(tenant.getParent());

            if(map.containsKey(tenant))
                continue;

            map.put(tenant, currentNode);

            if(parentNode != null)
                parentNode.getChildren().add(currentNode);

            if(Utils.isNotEmpty(tenant.getChildren()))
                tenant.getChildren().forEach(queue::offer);
        }

        List<NestedTenantsTreeNode> rootNodes = roots.stream().map(map::get).toList();

        return new DescribeTenantsTreeResponse(rootNodes);
    }
}
