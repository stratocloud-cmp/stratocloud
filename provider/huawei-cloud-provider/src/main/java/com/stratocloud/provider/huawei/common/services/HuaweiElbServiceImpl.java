package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.elb.v3.ElbClient;
import com.huaweicloud.sdk.elb.v3.model.*;
import com.huaweicloud.sdk.elb.v3.region.ElbRegion;
import com.stratocloud.cache.CacheService;
import com.stratocloud.cache.CacheUtil;
import com.stratocloud.provider.huawei.elb.member.HuaweiMember;
import com.stratocloud.provider.huawei.elb.member.MemberId;
import com.stratocloud.provider.huawei.elb.rule.HuaweiRule;
import com.stratocloud.provider.huawei.elb.rule.HuaweiRuleId;
import com.stratocloud.provider.huawei.elb.zone.HuaweiElbZoneSet;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class HuaweiElbServiceImpl extends HuaweiAbstractService implements HuaweiElbService{
    public HuaweiElbServiceImpl(CacheService cacheService, ICredential credential, String regionId, String accessKeyId) {
        super(cacheService, credential, regionId, accessKeyId);
    }

    private ElbClient buildClient(){
        return ElbClient.newBuilder()
                .withCredential(credential)
                .withRegion(ElbRegion.valueOf(regionId))
                .build();
    }

    @Override
    public Optional<LoadBalancer> describeLoadBalancer(String elbId) {
        return queryOne(
                () -> buildClient().showLoadBalancer(
                        new ShowLoadBalancerRequest().withLoadbalancerId(elbId)
                ).getLoadbalancer()
        );
    }

    @Override
    public List<LoadBalancer> describeLoadBalancers(ListLoadBalancersRequest request) {
        return queryAll(
                () -> buildClient().listLoadBalancers(request).getLoadbalancers(),
                request::setLimit,
                request::setMarker,
                LoadBalancer::getId
        );
    }

    @Override
    public String createLoadBalancer(CreateLoadBalancerRequest request) {
        String elbId = tryInvoke(
                () -> buildClient().createLoadBalancer(request)
        ).getLoadbalancer().getId();
        log.info("Huawei create elb request sent. ElbId={}.", elbId);
        return elbId;
    }


    @Override
    public Optional<Flavor> describeFlavor(String flavorId) {
        return queryOne(
                () -> buildClient().showFlavor(new ShowFlavorRequest().withFlavorId(flavorId)).getFlavor()
        );
    }

    @Override
    public List<Flavor> describeFlavors(ListFlavorsRequest request) {
        return queryAll(
                () -> buildClient().listFlavors(request).getFlavors(),
                request::setLimit,
                request::setMarker,
                Flavor::getId
        );
    }

    @Override
    public List<AvailabilityZone> describeZones(ListAvailabilityZonesRequest request) {
        return queryAll(
                () -> flattenZones(
                        buildClient().listAvailabilityZones(request).getAvailabilityZones()
                )
        );
    }

    private List<AvailabilityZone> flattenZones(List<List<AvailabilityZone>> zoneLists){
        List<AvailabilityZone> result = new ArrayList<>();

        if(Utils.isNotEmpty(zoneLists)){
            for (List<AvailabilityZone> zoneList : zoneLists) {
                if(Utils.isNotEmpty(zoneList)){
                    for (AvailabilityZone zone : zoneList) {
                        if(result.stream().anyMatch(z -> Objects.equals(z.getCode(), zone.getCode())))
                            continue;
                        result.add(zone);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public Optional<AvailabilityZone> describeZone(String zoneCode) {
        return describeZones(new ListAvailabilityZonesRequest()).stream().filter(
                z -> Objects.equals(zoneCode, z.getCode())
        ).findAny();
    }

    @Override
    public List<HuaweiElbZoneSet> describeZoneSets() {
        return queryAll(
                () -> buildClient().listAvailabilityZones(
                        new ListAvailabilityZonesRequest()
                ).getAvailabilityZones()
        ).stream().map(HuaweiElbZoneSet::new).toList();
    }

    @Override
    public Optional<HuaweiElbZoneSet> describeZoneSet(String zoneSetId) {
        return describeZoneSets().stream().filter(
                set -> Objects.equals(zoneSetId, set.getZoneSetId())
        ).findAny();
    }

    @Override
    public void cascadeDeleteElb(String elbId) {
        tryInvoke(
                () -> buildClient().deleteLoadBalancerForce(
                        new DeleteLoadBalancerForceRequest().withLoadbalancerId(elbId)
                )
        );

        log.info("Huawei cascade-delete elb request sent. ElbId={}.", elbId);
    }

    @Override
    public void associateElbToZone(String elbId, String zoneCode) {
        BatchAddAvailableZonesResponse response = tryInvoke(
                () -> buildClient().batchAddAvailableZones(
                        new BatchAddAvailableZonesRequest().withLoadbalancerId(elbId).withBody(
                                new BatchAddAvailableZonesRequestBody().withAvailabilityZoneList(List.of(zoneCode))
                        )
                )
        );

        log.info("Huawei elb batch-add zone request sent. RequestId={}. ElbId={}. ZoneCode={}.",
                response.getRequestId(), elbId, zoneCode);
    }

    @Override
    public void disassociateElbFromZone(String elbId, String zoneCode) {
        BatchRemoveAvailableZonesResponse response = tryInvoke(
                () -> buildClient().batchRemoveAvailableZones(
                        new BatchRemoveAvailableZonesRequest().withLoadbalancerId(elbId).withBody(
                                new BatchRemoveAvailableZonesRequestBody().withAvailabilityZoneList(List.of(zoneCode))
                        )
                )
        );

        log.info("Huawei elb batch-remove zone request sent. RequestId={}. ElbId={}. ZoneCode={}.",
                response.getRequestId(), elbId, zoneCode);
    }

    @Override
    public List<Listener> describeListeners(ListListenersRequest request) {
        return queryAll(
                () -> buildClient().listListeners(request).getListeners(),
                request::setLimit,
                request::setMarker,
                Listener::getId
        );
    }

    @Override
    public Optional<Listener> describeListener(String listenerId) {
        return queryOne(
                () -> buildClient().showListener(
                        new ShowListenerRequest().withListenerId(listenerId)
                ).getListener()
        );
    }

    @Override
    public String createListener(CreateListenerRequest request) {
        String listenerId = tryInvoke(
                () -> buildClient().createListener(request)
        ).getListener().getId();

        log.info("Huawei create listener request sent. ListenerId={}.",
                listenerId);

        return listenerId;
    }

    @Override
    public void deleteListener(String listenerId) {
        tryInvoke(
                () -> buildClient().deleteListener(
                        new DeleteListenerRequest().withListenerId(listenerId)
                )
        );

        log.info("Huawei delete listener request sent. ListenerId={}.",
                listenerId);
    }

    @Override
    public List<Pool> describeLbPools(ListPoolsRequest request) {
        return queryAll(
                () -> buildClient().listPools(request).getPools(),
                request::setLimit,
                request::setMarker,
                Pool::getId
        );
    }

    @Override
    public Optional<Pool> describeLbPool(String poolId) {
        return queryOne(
                () -> buildClient().showPool(new ShowPoolRequest().withPoolId(poolId)).getPool()
        );
    }

    @Override
    public Optional<LoadBalancerStatusResult> describeLbStatusTree(String elbId) {
        return Optional.ofNullable(
                CacheUtil.queryWithCache(
                        cacheService,
                        buildCacheKey("ElbStatusTree", Map.of("ElbId", elbId)),
                        30,
                        () -> doDescribeLbStatusTree(elbId),
                        new LoadBalancerStatusResult()
                )
        );
    }

    private LoadBalancerStatusResult doDescribeLbStatusTree(String elbId){
        return queryOne(
                () -> buildClient().showLoadBalancerStatus(
                        new ShowLoadBalancerStatusRequest().withLoadbalancerId(elbId)
                ).getStatuses()
        ).orElse(null);
    }

    @Override
    public Optional<HuaweiMember> describeMember(MemberId memberId) {
        return queryOne(
                () -> buildClient().showMember(
                        new ShowMemberRequest().withPoolId(memberId.poolId()).withMemberId(memberId.memberId())
                ).getMember()
        ).map(m -> new HuaweiMember(memberId, m));
    }

    @Override
    public List<HuaweiMember> describeMembers() {
        List<Pool> pools = describeLbPools(new ListPoolsRequest());

        List<HuaweiMember> result = new ArrayList<>();
        for (Pool pool : pools) {
            result.addAll(describeMembersByPoolId(pool.getId()));
        }
        return result;
    }

    private List<HuaweiMember> describeMembersByPoolId(String poolId){
        ListMembersRequest request = new ListMembersRequest().withPoolId(poolId);
        return queryAll(
                () -> buildClient().listMembers(request).getMembers(),
                request::setLimit,
                request::setMarker,
                Member::getId
        ).stream().map(
                m -> new HuaweiMember(new MemberId(poolId, m.getId()), m)
        ).toList();
    }

    @Override
    public Optional<HealthMonitor> describeMonitor(String monitorId) {
        return queryOne(
                () -> buildClient().showHealthMonitor(
                        new ShowHealthMonitorRequest().withHealthmonitorId(monitorId)
                ).getHealthmonitor()
        );
    }

    @Override
    public List<HealthMonitor> describeMonitors(ListHealthMonitorsRequest request) {
        return queryAll(
                () -> buildClient().listHealthMonitors(
                        new ListHealthMonitorsRequest()
                ).getHealthmonitors(),
                request::setLimit,
                request::setMarker,
                HealthMonitor::getId
        );
    }

    @Override
    public String createLbPool(CreatePoolRequest request) {
        String poolId = tryInvoke(
                () -> buildClient().createPool(request)
        ).getPool().getId();
        log.info("Huawei create ELB pool request sent. PoolId={}.", poolId);
        return poolId;
    }

    @Override
    public void deleteLbPool(String poolId) {
        tryInvoke(
                () -> buildClient().deletePool(
                        new DeletePoolRequest().withPoolId(poolId)
                )
        );
        log.info("Huawei delete ELB pool request sent. PoolId={}.", poolId);
    }

    @Override
    public MemberId createLbPoolMember(CreateMemberRequest request) {
        CreateMemberResponse response = tryInvoke(
                () -> buildClient().createMember(request)
        );

        log.info("Huawei create member request sent. MemberId={}.", response.getMember().getId());

        return new MemberId(request.getPoolId(), response.getMember().getId());
    }

    @Override
    public void deleteLbPoolMember(MemberId memberId) {
        tryInvoke(
                () -> buildClient().deleteMember(
                        new DeleteMemberRequest().withPoolId(memberId.poolId()).withMemberId(memberId.memberId())
                )
        );

        log.info("Huawei delete member request sent. MemberId={}.", memberId.memberId());
    }

    @Override
    public String createHealthMonitor(CreateHealthMonitorRequest request) {
        String monitorId = tryInvoke(
                () -> buildClient().createHealthMonitor(request)
        ).getHealthmonitor().getId();
        log.info("Huawei create health monitor request sent. MonitorId={}.", monitorId);
        return monitorId;
    }

    @Override
    public void deleteHealthMonitor(String monitorId) {
        tryInvoke(
                () -> buildClient().deleteHealthMonitor(
                        new DeleteHealthMonitorRequest().withHealthmonitorId(monitorId)
                )
        );
        log.info("Huawei delete health monitor request sent. MonitorId={}.", monitorId);
    }

    @Override
    public List<L7Policy> describePolicies(ListL7PoliciesRequest request) {
        return queryAll(
                () -> buildClient().listL7Policies(request).getL7policies(),
                request::setLimit,
                request::setMarker,
                L7Policy::getId
        );
    }

    @Override
    public Optional<L7Policy> describePolicy(String policyId) {
        return queryOne(
                () -> buildClient().showL7Policy(new ShowL7PolicyRequest().withL7policyId(policyId)).getL7policy()
        );
    }

    @Override
    public String createPolicy(CreateL7PolicyRequest request) {
        String policyId = tryInvoke(
                () -> buildClient().createL7Policy(request).getL7policy().getId()
        );

        log.info("Huawei create L7 policy request sent. PolicyId={}.", policyId);

        return policyId;
    }

    @Override
    public void deletePolicy(String policyId) {
        tryInvoke(
                () -> buildClient().deleteL7Policy(new DeleteL7PolicyRequest().withL7policyId(policyId))
        );

        log.info("Huawei delete L7 policy request sent. PolicyId={}.", policyId);
    }

    @Override
    public List<HuaweiRule> describeRules() {
        List<L7Policy> policies = describePolicies(
                new ListL7PoliciesRequest()
        );

        List<HuaweiRule> result = new ArrayList<>();

        for (L7Policy policy : policies) {
            ListL7RulesRequest request = new ListL7RulesRequest().withL7policyId(policy.getId());
            List<L7Rule> l7Rules = queryAll(
                    () -> buildClient().listL7Rules(request).getRules(),
                    request::setLimit,
                    request::setMarker,
                    L7Rule::getId
            );
            result.addAll(
                    l7Rules.stream().map(
                            r -> new HuaweiRule(
                                    new HuaweiRuleId(policy.getId(), r.getId()),
                                    r
                            )
                    ).toList()
            );
        }

        return result;
    }

    @Override
    public Optional<HuaweiRule> describeRule(HuaweiRuleId ruleId) {
        return queryOne(
                () -> buildClient().showL7Rule(
                        new ShowL7RuleRequest().withL7policyId(ruleId.policyId()).withL7ruleId(ruleId.ruleId())
                ).getRule()
        ).map(r -> new HuaweiRule(ruleId, r));
    }

    @Override
    public HuaweiRuleId createRule(CreateL7RuleRequest request) {
        String ruleId = tryInvoke(
                () -> buildClient().createL7Rule(request)
        ).getRule().getId();

        HuaweiRuleId huaweiRuleId = new HuaweiRuleId(request.getL7policyId(), ruleId);

        log.info("Huawei create ELB rule request sent. RuleId={}.", huaweiRuleId);

        return huaweiRuleId;
    }

    @Override
    public void deleteRule(HuaweiRuleId ruleId) {
        tryInvoke(
                () -> buildClient().deleteL7Rule(
                        new DeleteL7RuleRequest().withL7policyId(ruleId.policyId()).withL7ruleId(ruleId.ruleId())
                )
        );

        log.info("Huawei delete ELB rule request sent. RuleId={}.", ruleId);
    }
}
