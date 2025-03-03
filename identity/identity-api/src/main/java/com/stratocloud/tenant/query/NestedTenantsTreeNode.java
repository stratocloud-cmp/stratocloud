package com.stratocloud.tenant.query;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class NestedTenantsTreeNode {
    private NestedTenantResponse tenant;

    private List<NestedTenantsTreeNode> children = new ArrayList<>();

    public NestedTenantsTreeNode(NestedTenantResponse tenant) {
        this.tenant = tenant;
    }
}
