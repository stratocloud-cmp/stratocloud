package com.stratocloud.provider.tencent.common;

import com.stratocloud.provider.constants.SecurityGroupPolicyDirection;
import com.stratocloud.provider.tencent.flavor.TencentFlavorId;
import com.stratocloud.provider.tencent.lb.backend.TencentBackend;
import com.stratocloud.provider.tencent.lb.backend.TencentInstanceBackendId;
import com.stratocloud.provider.tencent.lb.backend.TencentNicBackendId;
import com.stratocloud.provider.tencent.lb.listener.TencentListener;
import com.stratocloud.provider.tencent.lb.listener.TencentListenerId;
import com.stratocloud.provider.tencent.lb.rule.TencentL7Rule;
import com.stratocloud.provider.tencent.lb.rule.TencentL7RuleId;
import com.stratocloud.provider.tencent.securitygroup.policy.TencentSecurityGroupPolicyId;
import com.tencentcloudapi.cam.v20190116.models.GetUserAppIdResponse;
import com.tencentcloudapi.cbs.v20170312.models.*;
import com.tencentcloudapi.cbs.v20170312.models.Snapshot;
import com.tencentcloudapi.clb.v20180317.models.*;
import com.tencentcloudapi.cvm.v20170312.models.Image;
import com.tencentcloudapi.cvm.v20170312.models.ZoneInfo;
import com.tencentcloudapi.cvm.v20170312.models.*;
import com.tencentcloudapi.monitor.v20180724.models.GetMonitorDataRequest;
import com.tencentcloudapi.monitor.v20180724.models.GetMonitorDataResponse;
import com.tencentcloudapi.ssl.v20191205.models.ApplyCertificateRequest;
import com.tencentcloudapi.ssl.v20191205.models.Certificates;
import com.tencentcloudapi.ssl.v20191205.models.CreateCertificateRequest;
import com.tencentcloudapi.ssl.v20191205.models.DescribeCertificatesRequest;
import com.tencentcloudapi.tat.v20201028.models.*;
import com.tencentcloudapi.vpc.v20170312.models.*;

import java.util.List;
import java.util.Optional;

public interface TencentCloudClient {

    String getRegion();

    void validateConnection();

    Float describeBalance();

    List<ZoneInfo> describeZones();

    Optional<ZoneInfo> describeZone(String zone);

    List<Vpc> describeVpcs(DescribeVpcsRequest request);

    Optional<Vpc> describeVpc(String vpcId);

    List<InstanceTypeConfig> describeInstanceTypes(DescribeInstanceTypeConfigsRequest request);

    Optional<InstanceTypeConfig> describeInstanceType(TencentFlavorId flavorId);

    Optional<InstanceTypeQuotaItem> describeInstanceTypeQuotaItem(TencentFlavorId flavorId);

    List<InstanceTypeQuotaItem> describeInstanceTypeQuotaItems(DescribeZoneInstanceConfigInfosRequest request);

    List<InstanceFamilyConfig> describeInstanceFamilies();

    List<Image> describeImages(DescribeImagesRequest request);

    Optional<Image> describeImage(String imageId);

    Optional<Subnet> describeSubnet(String subnetId);

    List<Subnet> describeSubnets(DescribeSubnetsRequest request);

    Optional<SecurityGroup> describeSecurityGroup(String securityGroupId);

    List<SecurityGroup> describeSecurityGroups(DescribeSecurityGroupsRequest request);

    SecurityGroupPolicy[] describeSecurityGroupPolicies(String securityGroupId,
                                                        SecurityGroupPolicyDirection direction);

    Optional<SecurityGroupPolicy> describeSecurityGroupPolicy(TencentSecurityGroupPolicyId policyId);

    List<SecurityGroupPolicy> describeSecurityGroupPolicies(SecurityGroupPolicyDirection direction);

    void createSecurityGroupPolicies(CreateSecurityGroupPoliciesRequest request);

    void removeSecurityGroupPolicy(TencentSecurityGroupPolicyId policyId);

    SecurityGroup createSecurityGroup(CreateSecurityGroupRequest request);

    void deleteSecurityGroup(String securityGroupId);

    Vpc createVpc(CreateVpcRequest request);

    void deleteVpc(String vpcId);

    Subnet createSubnet(CreateSubnetRequest request);

    void deleteSubnet(String subnetId);

    Optional<NetworkInterface> describeNic(String nicId);

    Optional<NetworkInterface> describePrimaryNicByInstanceId(String instanceId);

    List<NetworkInterface> describeNics(DescribeNetworkInterfacesRequest request);

    NetworkInterface createNic(CreateNetworkInterfaceRequest request);

    void deleteNic(String nicId);

    Optional<Instance> describeInstance(String instanceId);

    List<Instance> describeInstances(DescribeInstancesRequest request);

    String runInstance(RunInstancesRequest request);

    Optional<Disk> describeSystemDiskByInstanceId(String instanceId);

    void terminateInstance(String instanceId);

    List<Disk> describeDisks(DescribeDisksRequest request);

    Optional<Disk> describeDisk(String diskId);

    void attachNic(String instanceId, String nicId);

    void detachNic(String instanceId, String nicId);

    void modifyNic(ModifyNetworkInterfaceAttributeRequest modifyRequest);

    void modifyNicQosLevel(ModifyNetworkInterfaceQosRequest modifyQosRequest);

    void assignPrivateIps(AssignPrivateIpAddressesRequest assignRequest);

    String createDisk(CreateDisksRequest request);

    void modifyDisk(ModifyDiskAttributesRequest modifyRequest);

    void modifyDiskBackupQuota(ModifyDiskBackupQuotaRequest modifyBackupQuotaRequest);

    void modifyDiskExtraPerformance(ModifyDiskExtraPerformanceRequest modifyDiskExtraPerformanceRequest);

    void deleteDisk(String diskId);

    void attachDisk(String instanceId, String diskId);

    void detachDisk(String instanceId, String diskId);

    void resizeDisk(ResizeDiskRequest request);

    void resizeInstanceDisks(ResizeInstanceDisksRequest request);

    void resetInstanceType(ResetInstancesTypeRequest request);

    void resetInstance(ResetInstanceRequest request);

    void startInstance(String instanceId);

    void stopInstance(String instanceId, String stopType);

    void restartInstance(String instanceId, String stopType);

    Optional<KeyPair> describeKeyPair(String keyPairId);

    List<KeyPair> describeKeyPairs(DescribeKeyPairsRequest request);

    KeyPair createKeyPair(CreateKeyPairRequest request);

    void deleteKeyPair(String keyPairId);

    void associateKeyPairs(String instanceId, List<String> keyPairIds);

    void disassociateKeyPair(String instanceId, String keyPairId);

    Optional<Address> describeEip(String eipId);

    List<Address> describeEips(DescribeAddressesRequest request);

    String createEip(AllocateAddressesRequest request);

    Optional<BandwidthPackage> describeBandwidthPackage(String packageId);

    List<BandwidthPackage> describeBandwidthPackages(DescribeBandwidthPackagesRequest request);



    String createBandwidthPackage(CreateBandwidthPackageRequest request);

    void deleteBandwidthPackage(String packageId);

    void addBandwidthPackageResource(String packageId, String resourceId, String resourceType);

    void removeBandwidthPackageResource(String packageId, String resourceId, String resourceType);

    void associateNicToSecurityGroup(String nicId, String securityGroupId);

    void disassociateNicFromSecurityGroup(String nicId, String securityGroupId);

    void associateEipToNic(String eipId, String nicId, String privateIp);

    void disassociateEipFromNic(String eipId);

    com.tencentcloudapi.cvm.v20170312.models.Price inquiryPriceResetInstance(InquiryPriceResetInstanceRequest request);

    com.tencentcloudapi.cvm.v20170312.models.Price inquiryPriceRenewInstance(InquiryPriceRenewInstancesRequest request);


    com.tencentcloudapi.cvm.v20170312.models.Price inquiryPriceRunInstance(InquiryPriceRunInstancesRequest inquiry);

    PrepayPrice inquiryPriceRenewDisk(InquiryPriceRenewDisksRequest request);

    PrepayPrice inquiryPriceResizeDisk(InquiryPriceResizeDiskRequest request);

    com.tencentcloudapi.cbs.v20170312.models.Price inquiryPriceCreateDisk(InquiryPriceCreateDisksRequest inquiry);

    InstanceRefund inquiryPriceTerminateInstance(String instanceId);

    com.tencentcloudapi.cvm.v20170312.models.Price inquiryPriceResetInstanceType(InquiryPriceResetInstancesTypeRequest request);

    String describeInstanceConsoleUrl(String instanceId);


    Optional<LoadBalancer> describeLoadBalancer(String lbId);

    List<LoadBalancer> describeLoadBalancers(DescribeLoadBalancersRequest request);

    String createLoadBalancer(CreateLoadBalancerRequest request);

    void deleteLoadBalancer(String loadBalancerId);

    Optional<TencentListener> describeListener(TencentListenerId listenerId);

    List<TencentListener> describeListeners();

    String createListener(CreateListenerRequest request);

    void deleteListener(TencentListenerId listenerId);

    Optional<TencentBackend> describeBackend(TencentInstanceBackendId backendId);

    List<TencentBackend> describeBackends();

    Optional<TencentBackend> describeBackend(TencentNicBackendId backendId);

    Optional<TencentL7Rule> describeL7Rule(TencentL7RuleId ruleId);

    List<TencentL7Rule> describeL7Rules(TencentListenerId listenerId);

    List<TencentL7Rule> describeL7Rules();

    String createRule(CreateRuleRequest request);

    Optional<ListenerBackend> describeListenerBackend(TencentListenerId listenerId);

    void registerTarget(RegisterTargetsRequest request);

    void deregisterTarget(DeregisterTargetsRequest request);

    com.tencentcloudapi.clb.v20180317.models.Price inquiryPriceCreateLoadBalancer(InquiryPriceCreateLoadBalancerRequest request);

    Optional<Certificates> describeCert(String certId);

    List<Certificates> describeCerts(DescribeCertificatesRequest request);

    String applyFreeCertificate(ApplyCertificateRequest request);

    String createCertificate(CreateCertificateRequest request);

    void deleteCertificate(String certId);

    void deleteRule(DeleteRuleRequest request);

    void deleteEip(String eipId);

    GetMonitorDataResponse getMonitorData(GetMonitorDataRequest request);

    GetUserAppIdResponse getUserAppId();

    Optional<InvocationTask> describeInvocationTask(String taskId);

    List<Invocation> describeInvocations(DescribeInvocationsRequest request);

    Optional<Invocation> describeInvocation(String invocationId);

    Invocation runCommand(RunCommandRequest request);

    List<InvocationTask> describeInvocationTasks(DescribeInvocationTasksRequest request);

    void modifyInstancesAttribute(ModifyInstancesAttributeRequest request);

    void resetInstancesPassword(ResetInstancesPasswordRequest request);

    List<Snapshot> describeSnapshots(DescribeSnapshotsRequest request);

    Optional<Snapshot> describeSnapshot(String snapshotId);

    String createSnapshot(CreateSnapshotRequest request);

    void deleteSnapshot(String snapshotId);

    void rollbackToSnapshot(String diskId,
                            String snapshotId,
                            Boolean autoStop,
                            Boolean autoStart);
}
