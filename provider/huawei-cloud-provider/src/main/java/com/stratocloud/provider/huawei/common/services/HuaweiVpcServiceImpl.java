package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.vpc.v2.VpcClient;
import com.huaweicloud.sdk.vpc.v2.model.*;
import com.huaweicloud.sdk.vpc.v2.region.VpcRegion;
import com.stratocloud.cache.CacheService;
import com.stratocloud.cache.CacheUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class HuaweiVpcServiceImpl extends HuaweiAbstractService implements HuaweiVpcService {

    public HuaweiVpcServiceImpl(CacheService cacheService,
                                ICredential credential,
                                String regionId,
                                String accessKeyId) {
        super(cacheService, credential, regionId, accessKeyId);
    }

    private VpcClient buildClient(){
        return VpcClient.newBuilder()
                .withCredential(credential)
                .withRegion(VpcRegion.valueOf(regionId))
                .build();
    }


    @Override
    public Optional<Vpc> describeVpc(String vpcId) {
        return queryOne(
                () -> buildClient().showVpc(new ShowVpcRequest().withVpcId(vpcId)).getVpc()
        );
    }

    @Override
    public List<Vpc> describeVpcs(){
        return CacheUtil.queryWithCache(
                cacheService,
                buildCacheKey("Vpcs"),
                10L,
                this::doDescribeVpcs,
                new ArrayList<>()
        );
    }

    private List<Vpc> doDescribeVpcs() {
        ListVpcsRequest request = new ListVpcsRequest();
        return queryAll(
                () -> buildClient().listVpcs(request).getVpcs(),
                request::setLimit,
                request::setMarker,
                Vpc::getId
        );
    }

    @Override
    public String createVpc(CreateVpcRequest request) {
        String vpcId = tryInvoke(
                () -> buildClient().createVpc(request)
        ).getVpc().getId();
        log.info("Huawei create vpc request sent, vpcId={}.", vpcId);
        return vpcId;
    }

    @Override
    public void deleteVpc(String vpcId) {
        tryInvoke(
                () -> buildClient().deleteVpc(new DeleteVpcRequest().withVpcId(vpcId))
        );

        log.info("Huawei delete vpc request sent, vpcId={}.", vpcId);
    }


    @Override
    public Optional<Subnet> describeSubnet(String subnetId) {
        return queryOne(
                () -> buildClient().showSubnet(new ShowSubnetRequest().withSubnetId(subnetId)).getSubnet()
        );
    }

    @Override
    public List<Subnet> describeSubnets(ListSubnetsRequest request) {
        return CacheUtil.queryWithCache(
                cacheService,
                buildCacheKey("Subnets", request),
                300,
                () -> doDescribeSubnets(request),
                new ArrayList<>()
        );
    }

    private List<Subnet> doDescribeSubnets(ListSubnetsRequest request) {
        return queryAll(
                () -> buildClient().listSubnets(request).getSubnets(),
                request::setLimit,
                request::setMarker,
                Subnet::getId
        );
    }

    @Override
    public String createSubnet(CreateSubnetRequest request) {
        String subnetId = tryInvoke(
                () -> buildClient().createSubnet(request)
        ).getSubnet().getId();

        log.info("Huawei create subnet request sent, subnetId={}.", subnetId);

        return subnetId;
    }

    @Override
    public void deleteSubnet(Subnet subnet) {
        tryInvoke(
                () -> buildClient().deleteSubnet(
                        new DeleteSubnetRequest().withVpcId(subnet.getVpcId()).withSubnetId(subnet.getId())
                )
        );
        log.info("Huawei delete subnet request sent, subnetId={}.", subnet.getId());
    }

    @Override
    public Optional<SecurityGroup> describeSecurityGroup(String securityGroupId) {
        return queryOne(
                () -> buildClient().showSecurityGroup(
                        new ShowSecurityGroupRequest().withSecurityGroupId(securityGroupId)
                ).getSecurityGroup()
        );
    }

    @Override
    public List<SecurityGroup> describeSecurityGroups(ListSecurityGroupsRequest request) {
        return queryAll(
                () -> buildClient().listSecurityGroups(request).getSecurityGroups(),
                request::setLimit,
                request::setMarker,
                SecurityGroup::getId
        );
    }

    @Override
    public String createSecurityGroup(CreateSecurityGroupRequest request) {
        String securityGroupId = tryInvoke(
                () -> buildClient().createSecurityGroup(request)
        ).getSecurityGroup().getId();

        log.info("Huawei create security group request sent. SecurityGroupId={}.", securityGroupId);

        return securityGroupId;
    }

    @Override
    public void deleteSecurityGroup(String securityGroupId) {
        tryInvoke(
                () -> buildClient().deleteSecurityGroup(
                        new DeleteSecurityGroupRequest().withSecurityGroupId(securityGroupId)
                )
        );
        log.info("Huawei delete security group request sent. SecurityGroupId={}.", securityGroupId);
    }

    @Override
    public Optional<SecurityGroupRule> describeSecurityGroupRule(String ruleId) {
        return queryOne(
                () -> buildClient().showSecurityGroupRule(
                        new ShowSecurityGroupRuleRequest().withSecurityGroupRuleId(ruleId)
                ).getSecurityGroupRule()
        );
    }

    @Override
    public List<SecurityGroupRule> describeSecurityGroupRules(ListSecurityGroupRulesRequest request) {
        return queryAll(
                () -> buildClient().listSecurityGroupRules(request).getSecurityGroupRules(),
                request::setLimit,
                request::setMarker,
                SecurityGroupRule::getId
        );
    }

    @Override
    public String createSecurityGroupRule(CreateSecurityGroupRuleRequest request) {
        String ruleId = tryInvoke(
                () -> buildClient().createSecurityGroupRule(request)
        ).getSecurityGroupRule().getId();

        log.info("Huawei create security group rule request sent. SecurityGroupRuleId={}.",
                ruleId);

        return ruleId;
    }

    @Override
    public void deleteSecurityGroupRule(String ruleId) {
        tryInvoke(
                () -> buildClient().deleteSecurityGroupRule(
                        new DeleteSecurityGroupRuleRequest().withSecurityGroupRuleId(ruleId)
                )
        );
        log.info("Huawei delete security group rule request sent. SecurityGroupRuleId={}.",
                ruleId);
    }

    @Override
    public Optional<Port> describePort(String portId) {
        return queryOne(
                () -> buildClient().showPort(new ShowPortRequest().withPortId(portId)).getPort()
        );
    }

    @Override
    public List<Port> describePorts(ListPortsRequest request) {
        return queryAll(
                () -> buildClient().listPorts(request).getPorts(),
                request::setLimit,
                request::setMarker,
                Port::getId
        );
    }

    @Override
    public String createPort(CreatePortRequest request) {
        String portId = tryInvoke(
                () -> buildClient().createPort(request)
        ).getPort().getId();

        log.info("Huawei create port request sent. PortId={}.", portId);

        return portId;
    }

    @Override
    public void deletePort(String portId) {
        tryInvoke(
                () -> buildClient().deletePort(new DeletePortRequest().withPortId(portId))
        );
        log.info("Huawei delete port request sent. PortId={}.", portId);
    }
}
