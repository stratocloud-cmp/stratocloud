package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.elb.v3.model.*;
import com.stratocloud.provider.huawei.elb.member.HuaweiMember;
import com.stratocloud.provider.huawei.elb.member.MemberId;
import com.stratocloud.provider.huawei.elb.rule.HuaweiRule;
import com.stratocloud.provider.huawei.elb.rule.HuaweiRuleId;
import com.stratocloud.provider.huawei.elb.zone.HuaweiElbZoneSet;

import java.util.List;
import java.util.Optional;

public interface HuaweiElbService {
    Optional<LoadBalancer> describeLoadBalancer(String elbId);

    List<LoadBalancer> describeLoadBalancers(ListLoadBalancersRequest request);

    String createLoadBalancer(CreateLoadBalancerRequest request);

    Optional<Flavor> describeFlavor(String flavorId);

    List<Flavor> describeFlavors(ListFlavorsRequest request);

    List<AvailabilityZone> describeZones(ListAvailabilityZonesRequest request);

    Optional<AvailabilityZone> describeZone(String zoneCode);

    Optional<HuaweiElbZoneSet> describeZoneSet(String zoneSetId);

    List<HuaweiElbZoneSet> describeZoneSets();

    void cascadeDeleteElb(String elbId);

    void associateElbToZone(String elbId, String zoneCode);

    void disassociateElbFromZone(String elbId, String zoneCode);

    Optional<Listener> describeListener(String listenerId);

    List<Listener> describeListeners(ListListenersRequest request);

    String createListener(CreateListenerRequest request);

    void deleteListener(String listenerId);

    Optional<Pool> describeLbPool(String poolId);

    List<Pool> describeLbPools(ListPoolsRequest request);

    Optional<LoadBalancerStatusResult> describeLbStatusTree(String elbId);

    Optional<HuaweiMember> describeMember(MemberId memberId);

    List<HuaweiMember> describeMembers();

    Optional<HealthMonitor> describeMonitor(String monitorId);

    List<HealthMonitor> describeMonitors(ListHealthMonitorsRequest request);

    String createLbPool(CreatePoolRequest request);

    void deleteLbPool(String poolId);

    MemberId createLbPoolMember(CreateMemberRequest request);

    void deleteLbPoolMember(MemberId memberId);

    String createHealthMonitor(CreateHealthMonitorRequest request);

    void deleteHealthMonitor(String monitorId);

    Optional<L7Policy> describePolicy(String policyId);

    List<L7Policy> describePolicies(ListL7PoliciesRequest request);

    String createPolicy(CreateL7PolicyRequest request);

    void deletePolicy(String policyId);

    Optional<HuaweiRule> describeRule(HuaweiRuleId ruleId);

    List<HuaweiRule> describeRules();

    HuaweiRuleId createRule(CreateL7RuleRequest request);

    void deleteRule(HuaweiRuleId ruleId);
}
