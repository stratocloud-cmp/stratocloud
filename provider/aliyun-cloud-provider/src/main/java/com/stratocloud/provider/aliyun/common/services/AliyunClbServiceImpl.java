package com.stratocloud.provider.aliyun.common.services;

import com.aliyun.slb20140515.Client;
import com.aliyun.slb20140515.models.*;
import com.aliyun.teaopenapi.models.Config;
import com.stratocloud.cache.CacheService;
import com.stratocloud.exceptions.ExternalAccountInvalidException;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.aliyun.cert.AliyunServerCert;
import com.stratocloud.provider.aliyun.lb.classic.AliyunClb;
import com.stratocloud.provider.aliyun.lb.classic.acl.AliyunClbAcl;
import com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.AliyunClbBackend;
import com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.AliyunClbBackendId;
import com.stratocloud.provider.aliyun.lb.classic.backend.msgroup.AliyunClbMasterSlaveGroup;
import com.stratocloud.provider.aliyun.lb.classic.backend.msgroup.AliyunClbMasterSlaveGroupId;
import com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.AliyunClbServerGroup;
import com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.AliyunClbServerGroupId;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListener;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerId;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class AliyunClbServiceImpl extends AliyunAbstractService implements AliyunClbService {

    public AliyunClbServiceImpl(CacheService cacheService, Config config) {
        super(cacheService, config);
    }

    private Client buildClient(){
        try {
            return new Client(config);
        } catch (Exception e) {
            throw new ExternalAccountInvalidException(e.getMessage(), e);
        }
    }


    @Override
    public String createLoadBalancer(CreateLoadBalancerRequest request){
        request.setRegionId(config.getRegionId());

        CreateLoadBalancerResponseBody responseBody = tryInvoke(
                () -> buildClient().createLoadBalancer(request)
        ).getBody();

        log.info("Aliyun create clb request sent. RequestId={}. LbId={}.",
                responseBody.getRequestId(), responseBody.getLoadBalancerId());

        return responseBody.getLoadBalancerId();
    }


    @Override
    public List<AliyunClb> describeLoadBalancers(DescribeLoadBalancersRequest request) {
        request.setRegionId(config.getRegionId());

        return queryAll(
                () -> buildClient().describeLoadBalancers(request),
                resp -> resp.getBody().getLoadBalancers().getLoadBalancer(),
                resp -> resp.getBody().getTotalCount(),
                request::setPageNumber,
                request::setPageSize
        ).stream().map(AliyunClb::new).toList();
    }


    @Override
    public Optional<AliyunClb> describeLoadBalancer(String lbId) {
        DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest();
        request.setLoadBalancerId(lbId);
        return describeLoadBalancers(request).stream().findAny();
    }

    @Override
    public void deleteLoadBalancer(String lbId) {
        DeleteLoadBalancerRequest request = new DeleteLoadBalancerRequest();
        request.setLoadBalancerId(lbId);

        DeleteLoadBalancerResponseBody responseBody = tryInvoke(
                () -> buildClient().deleteLoadBalancer(request)
        ).getBody();

        log.info("Aliyun delete clb request sent. RequestId={}. LbId={}.",
                responseBody.getRequestId(), lbId);
    }

    @Override
    public void setLoadBalancerStatus(SetLoadBalancerStatusRequest request) {
        request.setRegionId(config.getRegionId());

        SetLoadBalancerStatusResponseBody responseBody = tryInvoke(
                () -> buildClient().setLoadBalancerStatus(request)
        ).getBody();

        log.info("Aliyun set clb status request sent. RequestId={}. LbId={}.",
                responseBody.getRequestId(), request.getLoadBalancerId());
    }


    @Override
    public Optional<AliyunListener> describeListener(AliyunListenerId listenerId) {
        DescribeLoadBalancerListenersRequest request = new DescribeLoadBalancerListenersRequest();
        request.setLoadBalancerId(List.of(listenerId.loadBalancerId()));
        request.setListenerProtocol(listenerId.protocol());
        request.setListenerPort(Integer.valueOf(listenerId.port()));
        return describeListeners(request).stream().findAny();
    }

    @Override
    public List<AliyunListener> describeListeners(DescribeLoadBalancerListenersRequest request) {
        request.setRegionId(config.getRegionId());
        return queryAllByToken(
                () -> buildClient().describeLoadBalancerListeners(request),
                resp -> resp.getBody().getListeners(),
                resp -> resp.getBody().getNextToken(),
                request::setNextToken
        ).stream().map(AliyunListener::new).toList();
    }

    @Override
    public void startListener(AliyunListenerId listenerId) {
        StartLoadBalancerListenerRequest request = new StartLoadBalancerListenerRequest();
        request.setRegionId(config.getRegionId());
        request.setLoadBalancerId(listenerId.loadBalancerId());
        request.setListenerProtocol(listenerId.protocol());
        request.setListenerPort(Integer.valueOf(listenerId.port()));

        String requestId = tryInvoke(
                () -> buildClient().startLoadBalancerListener(request)
        ).getBody().getRequestId();

        log.info("Aliyun start clb listener request sent. RequestId={}. Listener={}.",
                requestId, listenerId);
    }

    @Override
    public void stopListener(AliyunListenerId listenerId) {
        StopLoadBalancerListenerRequest request = new StopLoadBalancerListenerRequest();
        request.setRegionId(config.getRegionId());
        request.setLoadBalancerId(listenerId.loadBalancerId());
        request.setListenerProtocol(listenerId.protocol());
        request.setListenerPort(Integer.valueOf(listenerId.port()));

        String requestId = tryInvoke(
                () -> buildClient().stopLoadBalancerListener(request)
        ).getBody().getRequestId();

        log.info("Aliyun stop clb listener request sent. RequestId={}. Listener={}.",
                requestId, listenerId);
    }

    @Override
    public void createHttpListener(CreateLoadBalancerHTTPListenerRequest request) {
        request.setRegionId(config.getRegionId());
        CreateLoadBalancerHTTPListenerResponseBody responseBody = tryInvoke(
                () -> buildClient().createLoadBalancerHTTPListener(request)
        ).getBody();

        log.info("Aliyun create clb http listener request sent. RequestId={}.",
                responseBody.getRequestId());
    }

    @Override
    public void createHttpsListener(CreateLoadBalancerHTTPSListenerRequest request) {
        request.setRegionId(config.getRegionId());
        CreateLoadBalancerHTTPSListenerResponseBody responseBody = tryInvoke(
                () -> buildClient().createLoadBalancerHTTPSListener(request)
        ).getBody();

        log.info("Aliyun create clb https listener request sent. RequestId={}.",
                responseBody.getRequestId());
    }


    @Override
    public List<AliyunServerCert> describeCerts(DescribeServerCertificatesRequest request) {
        request.setRegionId(config.getRegionId());

        var certificates = tryInvoke(
                () -> buildClient().describeServerCertificates(request)
        ).getBody().getServerCertificates().getServerCertificate();

        if(Utils.isEmpty(certificates))
            return List.of();

        return certificates.stream().map(AliyunServerCert::new).toList();
    }

    @Override
    public Optional<AliyunServerCert> describeCert(String certId) {
        DescribeServerCertificatesRequest request = new DescribeServerCertificatesRequest();
        request.setRegionId(config.getRegionId());
        request.setServerCertificateId(certId);
        return describeCerts(request).stream().findAny();
    }


    @Override
    public Optional<AliyunClbAcl> describeAcl(String aclId) {
        return describeAclList(new DescribeAccessControlListsRequest()).stream().filter(
                acl -> Objects.equals(aclId, acl.detail().getAclId())
        ).findAny();
    }

    @Override
    public List<AliyunClbAcl> describeAclList(DescribeAccessControlListsRequest request){
        request.setRegionId(config.getRegionId());
        return queryAll(
                () -> buildClient().describeAccessControlLists(request),
                resp -> resp.getBody().getAcls().getAcl(),
                resp -> resp.getBody().getTotalCount(),
                request::setPageNumber,
                request::setPageSize
        ).stream().map(AliyunClbAcl::new).toList();
    }


    @Override
    public void setHttpListenerAttributes(SetLoadBalancerHTTPListenerAttributeRequest request) {
        request.setRegionId(config.getRegionId());

        var responseBody = tryInvoke(
                () -> buildClient().setLoadBalancerHTTPListenerAttribute(request)
        ).getBody();

        log.info("Aliyun modify clb http listener request sent. RequestId={}.",
                responseBody.getRequestId());
    }

    @Override
    public void setHttpsListenerAttributes(SetLoadBalancerHTTPSListenerAttributeRequest request) {
        request.setRegionId(config.getRegionId());

        var responseBody = tryInvoke(
                () -> buildClient().setLoadBalancerHTTPSListenerAttribute(request)
        ).getBody();

        log.info("Aliyun modify clb https listener request sent. RequestId={}.",
                responseBody.getRequestId());
    }

    @Override
    public void setTcpListenerAttributes(SetLoadBalancerTCPListenerAttributeRequest request) {
        request.setRegionId(config.getRegionId());

        var responseBody = tryInvoke(
                () -> buildClient().setLoadBalancerTCPListenerAttribute(request)
        ).getBody();

        log.info("Aliyun modify clb tcp listener request sent. RequestId={}.",
                responseBody.getRequestId());
    }

    @Override
    public void setUdpListenerAttributes(SetLoadBalancerUDPListenerAttributeRequest request) {
        request.setRegionId(config.getRegionId());

        var responseBody = tryInvoke(
                () -> buildClient().setLoadBalancerUDPListenerAttribute(request)
        ).getBody();

        log.info("Aliyun modify clb udp listener request sent. RequestId={}.",
                responseBody.getRequestId());
    }


    @Override
    public void createTcpListener(CreateLoadBalancerTCPListenerRequest request) {
        request.setRegionId(config.getRegionId());

        CreateLoadBalancerTCPListenerResponseBody responseBody = tryInvoke(
                () -> buildClient().createLoadBalancerTCPListener(request)
        ).getBody();

        log.info("Aliyun create tcp listener request sent. RequestId={}",
                responseBody.getRequestId());
    }

    @Override
    public void createUdpListener(CreateLoadBalancerUDPListenerRequest request) {
        request.setRegionId(config.getRegionId());

        CreateLoadBalancerUDPListenerResponseBody responseBody = tryInvoke(
                () -> buildClient().createLoadBalancerUDPListener(request)
        ).getBody();

        log.info("Aliyun create udp listener request sent. RequestId={}",
                responseBody.getRequestId());
    }




    @Override
    public List<AliyunClbBackend> describeBackends() {
        List<AliyunClb> clbList = describeLoadBalancers(new DescribeLoadBalancersRequest());

        if(Utils.isEmpty(clbList))
            return List.of();

        List<AliyunClbBackend> result = new ArrayList<>();
        for (AliyunClb clb : clbList) {
            List<AliyunClbBackend> backends = describeBackends(clb.detail().getLoadBalancerId());
            result.addAll(backends);
        }

        return result;
    }

    @Override
    public Optional<AliyunClbBackend> describeBackend(AliyunClbBackendId backendId) {
        String loadBalancerId = backendId.loadBalancerId();
        return describeBackends(loadBalancerId).stream().filter(
                backend -> backendId.equals(backend.id())
        ).findAny();
    }

    private List<AliyunClbBackend> describeBackends(String loadBalancerId){
        var lb = describeClbAttributes(loadBalancerId);

        var healthStatus = describeClbHealthStatus(loadBalancerId);

        if(lb.isEmpty() || healthStatus.isEmpty())
            return List.of();

        var backends = lb.get().getBackendServers().getBackendServer();

        var healthList = healthStatus.get().getBackendServers().getBackendServer();

        if(Utils.isEmpty(backends))
            return List.of();

        List<AliyunClbBackend> result = new ArrayList<>();

        for (var backend : backends) {
            if(Utils.isEmpty(healthList)){
                var health = new DescribeHealthStatusResponseBody.DescribeHealthStatusResponseBodyBackendServersBackendServer();
                health.setListenerPort(0);
                health.setPort(0);
                health.setProtocol("unknown");
                health.setServerHealthStatus("unavailable");
                health.setServerId(backend.getServerId());
                health.setServerIp(backend.getServerIp());

                AliyunClbBackendId backendId = new AliyunClbBackendId(
                        loadBalancerId, backend.getType(), backend.getServerId(), backend.getWeight()
                );

                result.add(new AliyunClbBackend(backendId, backend, health));
                continue;
            }

            for (var health : healthList) {
                boolean sameId = Objects.equals(backend.getServerId(), health.getServerId());
                if(sameId){
                    AliyunClbBackendId backendId = new AliyunClbBackendId(
                            loadBalancerId, backend.getType(), backend.getServerId(), backend.getWeight()
                    );

                    result.add(new AliyunClbBackend(backendId, backend, health));
                    break;
                }
            }
        }

        return result;
    }

    private Optional<DescribeLoadBalancerAttributeResponseBody> describeClbAttributes(String loadBalancerId) {
        var request = new DescribeLoadBalancerAttributeRequest();

        request.setRegionId(config.getRegionId());
        request.setLoadBalancerId(loadBalancerId);

        try {
            DescribeLoadBalancerAttributeResponseBody lb = tryInvoke(
                    () -> buildClient().describeLoadBalancerAttribute(request)
            ).getBody();
            return Optional.of(lb);
        }catch (ExternalResourceNotFoundException e){
            log.error("CLB {} does not exist.", loadBalancerId, e);
            return Optional.empty();
        }
    }

    private Optional<DescribeHealthStatusResponseBody> describeClbHealthStatus(String loadBalancerId) {
        var request = new DescribeHealthStatusRequest();

        request.setRegionId(config.getRegionId());
        request.setLoadBalancerId(loadBalancerId);

        try {
            DescribeHealthStatusResponseBody lb = tryInvoke(
                    () -> buildClient().describeHealthStatus(request)
            ).getBody();
            return Optional.of(lb);
        }catch (ExternalResourceNotFoundException e){
            log.error("CLB {} does not exist.", loadBalancerId, e);
            return Optional.empty();
        }
    }


    @Override
    public void addBackendServers(AddBackendServersRequest request) {
        request.setRegionId(config.getRegionId());

        AddBackendServersResponseBody responseBody = tryInvoke(() -> buildClient().addBackendServers(request)).getBody();

        log.info("Aliyun add clb backends request sent. RequestId={}",
                responseBody.getRequestId());
    }

    @Override
    public void removeBackendServers(RemoveBackendServersRequest request) {
        request.setRegionId(config.getRegionId());

        RemoveBackendServersResponseBody responseBody = tryInvoke(
                () -> buildClient().removeBackendServers(request)
        ).getBody();

        log.info("Aliyun remove clb backends request sent. RequestId={}",
                responseBody.getRequestId());
    }

    @Override
    public Optional<AliyunClbServerGroup> describeServerGroup(AliyunClbServerGroupId serverGroupId) {
        String loadBalancerId = serverGroupId.loadBalancerId();

        List<AliyunClbServerGroup> serverGroups = describeServerGroups(loadBalancerId);

        return serverGroups.stream().filter(
                group -> serverGroupId.equals(group.id())
        ).findAny();
    }

    private List<AliyunClbServerGroup> describeServerGroups(String loadBalancerId) {
        DescribeVServerGroupsRequest request = new DescribeVServerGroupsRequest();
        request.setRegionId(config.getRegionId());
        request.setLoadBalancerId(loadBalancerId);

        var serverGroups = tryInvoke(
                () -> buildClient().describeVServerGroups(request)
        ).getBody().getVServerGroups().getVServerGroup();

        if(Utils.isEmpty(serverGroups))
            return List.of();

        return serverGroups.stream().map(
                group -> {
                    String vServerGroupId = group.getVServerGroupId();
                    var responseBody = describeServerGroupAttributes(vServerGroupId);
                    return new AliyunClbServerGroup(
                            new AliyunClbServerGroupId(loadBalancerId, vServerGroupId),
                            group,
                            responseBody
                    );
                }
        ).toList();
    }

    private DescribeVServerGroupAttributeResponseBody describeServerGroupAttributes(String vServerGroupId) {
        DescribeVServerGroupAttributeRequest attributeRequest = new DescribeVServerGroupAttributeRequest();
        attributeRequest.setRegionId(config.getRegionId());
        attributeRequest.setVServerGroupId(vServerGroupId);
        return tryInvoke(
                () -> buildClient().describeVServerGroupAttribute(attributeRequest)
        ).getBody();
    }

    @Override
    public List<AliyunClbServerGroup> describeServerGroups(){
        List<AliyunClb> clbList = describeLoadBalancers(new DescribeLoadBalancersRequest());

        if(Utils.isEmpty(clbList))
            return List.of();

        List<AliyunClbServerGroup> result = new ArrayList<>();

        for (AliyunClb clb : clbList) {
            List<AliyunClbServerGroup> serverGroups = describeServerGroups(clb.detail().getLoadBalancerId());
            result.addAll(serverGroups);
        }

        return result;
    }

    @Override
    public AliyunClbServerGroupId createServerGroup(CreateVServerGroupRequest request) {
        request.setRegionId(config.getRegionId());

        CreateVServerGroupResponseBody responseBody = tryInvoke(
                () -> buildClient().createVServerGroup(request)
        ).getBody();

        log.info("Aliyun create clb server group request sent. RequestId={}.",
                responseBody.getRequestId());

        return new AliyunClbServerGroupId(request.getLoadBalancerId(), responseBody.getVServerGroupId());
    }

    @Override
    public void deleteServerGroup(AliyunClbServerGroupId id) {
        DeleteVServerGroupRequest request = new DeleteVServerGroupRequest();
        request.setRegionId(config.getRegionId());
        request.setVServerGroupId(id.serverGroupId());

        DeleteVServerGroupResponseBody responseBody = tryInvoke(
                () -> buildClient().deleteVServerGroup(request)
        ).getBody();

        log.info("Aliyun delete clb server group request sent. RequestId={}. ServerGroupId={}.",
                responseBody.getRequestId(), id);
    }

    @Override
    public void addServerGroupBackendServer(AddVServerGroupBackendServersRequest request) {
        request.setRegionId(config.getRegionId());

        AddVServerGroupBackendServersResponseBody responseBody = tryInvoke(
                () -> buildClient().addVServerGroupBackendServers(request)
        ).getBody();

        log.info("Aliyun add clb server group member request sent. RequestId={}. ServerGroupId={}.",
                responseBody.getRequestId(), request.getVServerGroupId());
    }

    @Override
    public void removeServerGroupBackendServer(RemoveVServerGroupBackendServersRequest request) {
        request.setRegionId(config.getRegionId());

        RemoveVServerGroupBackendServersResponseBody responseBody = tryInvoke(
                () -> buildClient().removeVServerGroupBackendServers(request)
        ).getBody();

        log.info("Aliyun remove clb server group member request sent. RequestId={}. ServerGroupId={}.",
                responseBody.getRequestId(), request.getVServerGroupId());
    }


    @Override
    public List<AliyunClbMasterSlaveGroup> describeMasterSlaveGroups() {
        List<AliyunClb> clbList = describeLoadBalancers(new DescribeLoadBalancersRequest());


        List<AliyunClbMasterSlaveGroup> result = new ArrayList<>();
        if(Utils.isEmpty(clbList))
            return result;

        for (AliyunClb clb : clbList) {
            List<AliyunClbMasterSlaveGroup> masterSlaveGroups
                    = describeMasterSlaveGroups(clb.detail().getLoadBalancerId());
            result.addAll(masterSlaveGroups);
        }

        return result;
    }

    @Override
    public Optional<AliyunClbMasterSlaveGroup> describeMasterSlaveGroup(AliyunClbMasterSlaveGroupId masterSlaveGroupId) {
        return describeMasterSlaveGroups(masterSlaveGroupId.loadBalancerId()).stream().filter(
                group -> masterSlaveGroupId.equals(group.id())
        ).findAny();
    }

    private List<AliyunClbMasterSlaveGroup> describeMasterSlaveGroups(String loadBalancerId){
        DescribeMasterSlaveServerGroupsRequest request = new DescribeMasterSlaveServerGroupsRequest();
        request.setRegionId(config.getRegionId());
        request.setLoadBalancerId(loadBalancerId);

        var groups = tryInvoke(
                () -> buildClient().describeMasterSlaveServerGroups(request)
        ).getBody().getMasterSlaveServerGroups().getMasterSlaveServerGroup();

        List<AliyunClbMasterSlaveGroup> result = new ArrayList<>();

        if(Utils.isEmpty(groups))
            return result;


        for (var group : groups) {
            var attributeRequest = new DescribeMasterSlaveServerGroupAttributeRequest();
            attributeRequest.setRegionId(config.getRegionId());
            attributeRequest.setMasterSlaveServerGroupId(group.getMasterSlaveServerGroupId());
            var body = tryInvoke(
                    () -> buildClient().describeMasterSlaveServerGroupAttribute(attributeRequest)
            ).getBody();

            result.add(new AliyunClbMasterSlaveGroup(
                    new AliyunClbMasterSlaveGroupId(loadBalancerId, group.getMasterSlaveServerGroupId()),
                    group,
                    body
            ));
        }

        return result;
    }


    @Override
    public AliyunClbMasterSlaveGroupId createMasterSlaveGroup(CreateMasterSlaveServerGroupRequest request) {
        request.setRegionId(config.getRegionId());

        CreateMasterSlaveServerGroupResponseBody responseBody = tryInvoke(
                () -> buildClient().createMasterSlaveServerGroup(request)
        ).getBody();

        log.info("Aliyun create master-slave group request sent. RequestId={}. GroupId={}.",
                responseBody.getRequestId(), responseBody.getMasterSlaveServerGroupId());


        return new AliyunClbMasterSlaveGroupId(
                request.getLoadBalancerId(),
                responseBody.getMasterSlaveServerGroupId()
        );
    }

    @Override
    public void deleteMasterSlaveGroup(AliyunClbMasterSlaveGroupId id) {
        DeleteMasterSlaveServerGroupRequest request = new DeleteMasterSlaveServerGroupRequest();
        request.setRegionId(config.getRegionId());

        request.setMasterSlaveServerGroupId(id.masterSlaveGroupId());

        DeleteMasterSlaveServerGroupResponseBody responseBody = tryInvoke(
                () -> buildClient().deleteMasterSlaveServerGroup(request)
        ).getBody();

        log.info("Aliyun delete master-slave group request sent. RequestId={}. GroupId={}.",
                responseBody.getRequestId(), id);
    }

    @Override
    public void deleteListener(AliyunListenerId listenerId) {
        DeleteLoadBalancerListenerRequest request = new DeleteLoadBalancerListenerRequest();
        request.setRegionId(config.getRegionId());
        request.setLoadBalancerId(listenerId.loadBalancerId());
        request.setListenerPort(Integer.valueOf(listenerId.port()));
        request.setListenerProtocol(listenerId.protocol());

        var responseBody = tryInvoke(() -> buildClient().deleteLoadBalancerListener(request)).getBody();

        log.info("Aliyun delete listener request sent. RequestId={}. ListenerId={}.",
                responseBody.getRequestId(), listenerId);
    }
}
