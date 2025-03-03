package com.stratocloud.provider.aliyun.common.services;

import com.aliyun.slb20140515.models.*;
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

import java.util.List;
import java.util.Optional;

public interface AliyunClbService {
    String createLoadBalancer(CreateLoadBalancerRequest request);

    Optional<AliyunClb> describeLoadBalancer(String lbId);

    List<AliyunClb> describeLoadBalancers(DescribeLoadBalancersRequest request);

    void deleteLoadBalancer(String lbId);

    void setLoadBalancerStatus(SetLoadBalancerStatusRequest request);

    Optional<AliyunListener> describeListener(AliyunListenerId listenerId);

    List<AliyunListener> describeListeners(DescribeLoadBalancerListenersRequest request);

    void startListener(AliyunListenerId listenerId);

    void stopListener(AliyunListenerId listenerId);

    void createHttpListener(CreateLoadBalancerHTTPListenerRequest request);

    void createHttpsListener(CreateLoadBalancerHTTPSListenerRequest request);

    Optional<AliyunServerCert> describeCert(String certId);

    List<AliyunServerCert> describeCerts(DescribeServerCertificatesRequest request);

    Optional<AliyunClbAcl> describeAcl(String aclId);

    List<AliyunClbAcl> describeAclList(DescribeAccessControlListsRequest request);

    void setHttpListenerAttributes(SetLoadBalancerHTTPListenerAttributeRequest request);

    void setHttpsListenerAttributes(SetLoadBalancerHTTPSListenerAttributeRequest request);

    void setTcpListenerAttributes(SetLoadBalancerTCPListenerAttributeRequest request);

    void setUdpListenerAttributes(SetLoadBalancerUDPListenerAttributeRequest request);

    void createTcpListener(CreateLoadBalancerTCPListenerRequest request);

    void createUdpListener(CreateLoadBalancerUDPListenerRequest request);

    Optional<AliyunClbBackend> describeBackend(AliyunClbBackendId backendId);

    List<AliyunClbBackend> describeBackends();

    void addBackendServers(AddBackendServersRequest request);

    void removeBackendServers(RemoveBackendServersRequest request);

    Optional<AliyunClbServerGroup> describeServerGroup(AliyunClbServerGroupId serverGroupId);

    List<AliyunClbServerGroup> describeServerGroups();

    AliyunClbServerGroupId createServerGroup(CreateVServerGroupRequest request);

    void deleteServerGroup(AliyunClbServerGroupId id);

    void addServerGroupBackendServer(AddVServerGroupBackendServersRequest request);

    void removeServerGroupBackendServer(RemoveVServerGroupBackendServersRequest request);

    Optional<AliyunClbMasterSlaveGroup> describeMasterSlaveGroup(AliyunClbMasterSlaveGroupId masterSlaveGroupId);

    List<AliyunClbMasterSlaveGroup> describeMasterSlaveGroups();

    AliyunClbMasterSlaveGroupId createMasterSlaveGroup(CreateMasterSlaveServerGroupRequest request);

    void deleteMasterSlaveGroup(AliyunClbMasterSlaveGroupId id);

    void deleteListener(AliyunListenerId listenerId);
}
