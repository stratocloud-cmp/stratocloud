package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.vpc.v2.model.*;

import java.util.List;
import java.util.Optional;

public interface HuaweiVpcService {
    Optional<Vpc> describeVpc(String vpcId);

    List<Vpc> describeVpcs();

    String createVpc(CreateVpcRequest request);

    void deleteVpc(String vpcId);

    Optional<Subnet> describeSubnet(String subnetId);

    List<Subnet> describeSubnets(ListSubnetsRequest request);

    String createSubnet(CreateSubnetRequest request);

    void deleteSubnet(Subnet subnet);

    Optional<SecurityGroup> describeSecurityGroup(String securityGroupId);

    List<SecurityGroup> describeSecurityGroups(ListSecurityGroupsRequest request);

    String createSecurityGroup(CreateSecurityGroupRequest request);

    void deleteSecurityGroup(String securityGroupId);

    Optional<SecurityGroupRule> describeSecurityGroupRule(String ruleId);

    List<SecurityGroupRule> describeSecurityGroupRules(ListSecurityGroupRulesRequest request);

    String createSecurityGroupRule(CreateSecurityGroupRuleRequest request);

    void deleteSecurityGroupRule(String ruleId);

    Optional<Port> describePort(String portId);

    List<Port> describePorts(ListPortsRequest request);

    String createPort(CreatePortRequest request);

    void deletePort(String portId);
}
