package com.stratocloud.provider.huawei.elb;

import com.huaweicloud.sdk.elb.v3.model.*;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.elb.member.MemberId;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceState;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class HuaweiLbStatusTreeHelper {

    public static void synchronizeLbStatusTree(Resource lbResource) {
        Optional<LoadBalancerStatusResult> statusTree = describeLbStatusTree(lbResource);

        if(statusTree.isEmpty())
            return;

        List<LoadBalancerStatusListener> listenerStatuses = statusTree.get().getLoadbalancer().getListeners();

        List<Resource> listenerResources = lbResource.getCapabilitySources(ResourceCategories.LOAD_BALANCER_LISTENER);

        if(Utils.isEmpty(listenerStatuses) || Utils.isEmpty(listenerResources))
            return;

        for (LoadBalancerStatusListener listenerStatus : listenerStatuses) {
            Optional<Resource> listenerResource = listenerResources.stream().filter(
                    r -> Objects.equals(listenerStatus.getId(), r.getExternalId())
            ).findAny();

            if(listenerResource.isEmpty())
                continue;

            synchronizeListenerStatusTree(listenerResource.get(), listenerStatus);
        }
    }

    private static Optional<LoadBalancerStatusResult> describeLbStatusTree(Resource lbResource) {
        Long accountId = lbResource.getAccountId();
        String elbId = lbResource.getExternalId();
        HuaweiCloudProvider provider = (HuaweiCloudProvider) lbResource.getResourceHandler().getProvider();
        return describeLbStatusTree(provider, accountId, elbId);
    }

    private static Optional<LoadBalancerStatusResult> describeLbStatusTree(HuaweiCloudProvider provider,
                                                                           Long accountId,
                                                                           String elbId) {
        if(Utils.isBlank(elbId))
            return Optional.empty();

        ExternalAccount account = provider.getAccountRepository().findExternalAccount(accountId);

        return provider.buildClient(account).elb().describeLbStatusTree(elbId);
    }

    private static Optional<LoadBalancerStatusListener> getListenerStatusFromTree(LoadBalancerStatusResult statusTree,
                                                                                  String listenerId){
        List<LoadBalancerStatusListener> listenerStatuses = statusTree.getLoadbalancer().getListeners();

        if(Utils.isEmpty(listenerStatuses))
            return Optional.empty();

        return listenerStatuses.stream().filter(
                s -> Objects.equals(s.getId(), listenerId)
        ).findAny();
    }

    private static Optional<LoadBalancerStatusPool> getPoolStatusFromTree(LoadBalancerStatusResult statusTree,
                                                                          String poolId){
        List<LoadBalancerStatusListener> listenerStatuses = statusTree.getLoadbalancer().getListeners();

        if(Utils.isEmpty(listenerStatuses))
            return Optional.empty();

        for (LoadBalancerStatusListener listenerStatus : listenerStatuses) {
            if(Utils.isEmpty(listenerStatus.getPools()))
                continue;

            Optional<LoadBalancerStatusPool> poolStatus = listenerStatus.getPools().stream().filter(
                    p -> Objects.equals(p.getId(), poolId)
            ).findAny();

            if(poolStatus.isEmpty())
                continue;

            return poolStatus;
        }

        return Optional.empty();
    }


    private static void synchronizeListenerStatusTree(Resource listenerResource,
                                                      LoadBalancerStatusListener listenerStatus) {
        listenerResource.setState(convertListenerState(listenerStatus.getOperatingStatus()));

        List<Resource> pools = listenerResource.getCapabilitySources(ResourceCategories.LOAD_BALANCER_BACKEND_GROUP);

        List<LoadBalancerStatusPool> poolStatusList = listenerStatus.getPools();

        if(Utils.isEmpty(pools) || Utils.isEmpty(poolStatusList))
            return;

        for (LoadBalancerStatusPool poolV2Status : poolStatusList) {
            Optional<Resource> pool = pools.stream().filter(
                    p -> Objects.equals(p.getExternalId(), poolV2Status.getId())
            ).findAny();

            if(pool.isEmpty())
                continue;

            synchronizePoolStatusTree(pool.get(), poolV2Status);
        }
    }

    private static void synchronizePoolStatusTree(Resource poolResource, LoadBalancerStatusPool poolStatus) {
        poolResource.setState(convertPoolState(poolStatus.getOperatingStatus()));

        List<Resource> members = poolResource.getCapabilitySources(ResourceCategories.LOAD_BALANCER_BACKEND);

        List<LoadBalancerStatusMember> memberStatuses = poolStatus.getMembers();

        if(Utils.isNotEmpty(members) && Utils.isNotEmpty(memberStatuses)){
            for (LoadBalancerStatusMember memberStatus : memberStatuses) {
                Optional<Resource> member = members.stream().filter(
                        m -> Objects.equals(
                                MemberId.fromString(m.getExternalId()).memberId(),
                                memberStatus.getId()
                        )
                ).findAny();

                if(member.isEmpty())
                    continue;

                synchronizeMemberStatus(member.get(), memberStatus);
            }
        }

        var monitor = poolResource.getPrimaryCapability(ResourceCategories.LOAD_BALANCER_HEALTH_MONITOR);
        LoadBalancerStatusHealthMonitor heathMonitorStatus = poolStatus.getHealthmonitor();

        if(monitor.isPresent() && heathMonitorStatus != null){
            synchronizeHealthMonitorStatus(monitor.get(), heathMonitorStatus);
        }
    }

    private static void synchronizeHealthMonitorStatus(Resource monitor, LoadBalancerStatusHealthMonitor heathMonitorStatus) {
        monitor.setState(convertHealthMonitorStatus(heathMonitorStatus.getProvisioningStatus()));
    }

    private static Optional<LoadBalancerStatusHealthMonitor> getHealthMonitorStatusFromTree(LoadBalancerStatusResult statusTree,
                                                                                            String monitorId){
        List<LoadBalancerStatusListener> listenerStatuses = statusTree.getLoadbalancer().getListeners();

        if(Utils.isEmpty(listenerStatuses))
            return Optional.empty();

        for (LoadBalancerStatusListener listenerStatus : listenerStatuses) {
            if(Utils.isEmpty(listenerStatus.getPools()))
                continue;

            for (LoadBalancerStatusPool poolStatus : listenerStatus.getPools()) {
                if(poolStatus.getHealthmonitor() == null)
                    continue;

                if(Objects.equals(monitorId, poolStatus.getHealthmonitor().getId()))
                    return Optional.of(poolStatus.getHealthmonitor());
            }
        }

        return Optional.empty();
    }

    public static void synchronizeListenerStatusTree(Resource listener){
        if(Utils.isBlank(listener.getExternalId()))
            return;

        Optional<Resource> loadBalancer = listener.getEssentialTarget(ResourceCategories.LOAD_BALANCER);

        if(loadBalancer.isEmpty())
            return;

        var statusTree = describeLbStatusTree(loadBalancer.get());

        if(statusTree.isEmpty())
            return;

        var listenerStatus = getListenerStatusFromTree(statusTree.get(), listener.getExternalId());

        if(listenerStatus.isEmpty())
            return;

        synchronizeListenerStatusTree(listener, listenerStatus.get());
    }

    public static void synchronizePoolStatusTree(Resource pool){
        if(Utils.isBlank(pool.getExternalId()))
            return;

        Optional<Resource> listener = pool.getEssentialTarget(ResourceCategories.LOAD_BALANCER_LISTENER);

        if(listener.isEmpty())
            return;

        Optional<Resource> loadBalancer = listener.get().getEssentialTarget(ResourceCategories.LOAD_BALANCER);

        if(loadBalancer.isEmpty())
            return;

        var statusTree = describeLbStatusTree(loadBalancer.get());

        if(statusTree.isEmpty())
            return;

        Optional<LoadBalancerStatusPool> poolStatus = getPoolStatusFromTree(statusTree.get(), pool.getExternalId());

        if(poolStatus.isEmpty())
            return;

        synchronizePoolStatusTree(pool, poolStatus.get());
    }



    private static void synchronizeMemberStatus(Resource memberResource, LoadBalancerStatusMember memberStatus) {
        memberResource.setState(convertMemberState(memberStatus.getOperatingStatus()));
    }


    private static ResourceState convertHealthMonitorStatus(String provisioningStatus) {
        if(provisioningStatus == null)
            return ResourceState.UNKNOWN;

        return switch (provisioningStatus) {
            case "ACTIVE" -> ResourceState.STARTED;
            case "DELETED" -> ResourceState.DESTROYED;
            case "ERROR" -> ResourceState.ERROR;
            case "PENDING_CREATE" -> ResourceState.BUILDING;
            case "PENDING_UPDATE" -> ResourceState.CONFIGURING;
            case "PENDING_DELETE" -> ResourceState.DESTROYING;
            default -> ResourceState.UNKNOWN;
        };
    }



    private static ResourceState convertMemberState(String operatingStatus) {
        return convertOperatingStatus(operatingStatus);
    }

    private static ResourceState convertPoolState(String operatingStatus) {
        return convertOperatingStatus(operatingStatus);
    }

    private static ResourceState convertListenerState(String operatingStatus) {
        return convertOperatingStatus(operatingStatus);
    }

    private static ResourceState convertOperatingStatus(String operatingStatus) {
        if(operatingStatus == null)
            return ResourceState.UNKNOWN;

        return switch (operatingStatus) {
            case "ONLINE" -> ResourceState.HEALTH_CHECK_NORMAL;
            case "DRAINING", "DEGRADED", "OFFLINE" -> ResourceState.HEALTH_CHECK_ABNORMAL;
            case "ERROR" -> ResourceState.ERROR;
            case "NO_MONITOR" -> ResourceState.HEALTH_CHECK_UNAVAILABLE;
            default -> ResourceState.UNKNOWN;
        };
    }


    public static ResourceState getListenerState(HuaweiCloudProvider provider,
                                                 Long accountId,
                                                 Listener listener){
        try {
            List<LoadBalancerRef> loadbalancers = listener.getLoadbalancers();

            if(Utils.isEmpty(loadbalancers))
                return ResourceState.HEALTH_CHECK_UNAVAILABLE;

            String elbId = loadbalancers.get(0).getId();

            Optional<LoadBalancerStatusResult> statusTree = describeLbStatusTree(provider, accountId, elbId);

            if(statusTree.isEmpty())
                return ResourceState.HEALTH_CHECK_UNAVAILABLE;

            var listenerStatus = getListenerStatusFromTree(statusTree.get(), listener.getId());

            if(listenerStatus.isEmpty())
                return ResourceState.HEALTH_CHECK_UNAVAILABLE;

            return convertListenerState(listenerStatus.get().getOperatingStatus());
        }catch (Exception e){
            log.warn(e.toString());
            return ResourceState.UNKNOWN;
        }
    }

    public static ResourceState getPoolState(HuaweiCloudProvider provider,
                                             Long accountId,
                                             Pool pool){
        try {
            List<LoadBalancerRef> loadbalancers = pool.getLoadbalancers();

            if(Utils.isEmpty(loadbalancers))
                return ResourceState.HEALTH_CHECK_UNAVAILABLE;

            String elbId = loadbalancers.get(0).getId();

            Optional<LoadBalancerStatusResult> statusTree = describeLbStatusTree(provider, accountId, elbId);

            if(statusTree.isEmpty())
                return ResourceState.HEALTH_CHECK_UNAVAILABLE;

            Optional<LoadBalancerStatusPool> poolStatus = getPoolStatusFromTree(statusTree.get(), pool.getId());

            if(poolStatus.isEmpty())
                return ResourceState.HEALTH_CHECK_UNAVAILABLE;

            return convertPoolState(poolStatus.get().getOperatingStatus());
        }catch (Exception e){
            log.warn(e.toString());
            return ResourceState.UNKNOWN;
        }
    }

    public static ResourceState getMemberState(Member member) {
        return convertMemberState(member.getOperatingStatus());
    }

    public static ResourceState getHealthMonitorState(HuaweiCloudProvider provider,
                                                      Long accountId,
                                                      HealthMonitor healthMonitor){
        try {
            List<PoolRef> pools = healthMonitor.getPools();

            if(Utils.isEmpty(pools))
                return ResourceState.UNKNOWN;

            String poolId = pools.get(0).getId();

            ExternalAccount account = provider.getAccountRepository().findExternalAccount(accountId);
            Optional<Pool> pool = provider.buildClient(account).elb().describeLbPool(poolId);

            if(pool.isEmpty())
                return ResourceState.UNKNOWN;

            List<LoadBalancerRef> loadbalancers = pool.get().getLoadbalancers();

            if(Utils.isEmpty(loadbalancers))
                return ResourceState.UNKNOWN;

            String elbId = loadbalancers.get(0).getId();

            Optional<LoadBalancerStatusResult> statusTree = describeLbStatusTree(provider, accountId, elbId);

            if(statusTree.isEmpty())
                return ResourceState.UNKNOWN;

            var monitorStatus = getHealthMonitorStatusFromTree(statusTree.get(), healthMonitor.getId());

            if(monitorStatus.isEmpty())
                return ResourceState.UNKNOWN;

            return convertHealthMonitorStatus(monitorStatus.get().getProvisioningStatus());
        }catch (Exception e){
            log.warn(e.toString());
            return ResourceState.UNKNOWN;
        }
    }


}
