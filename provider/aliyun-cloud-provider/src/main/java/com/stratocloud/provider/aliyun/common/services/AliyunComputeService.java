package com.stratocloud.provider.aliyun.common.services;

import com.aliyun.ecs20140526.models.*;
import com.stratocloud.provider.aliyun.disk.AliyunDisk;
import com.stratocloud.provider.aliyun.flavor.AliyunFlavor;
import com.stratocloud.provider.aliyun.flavor.AliyunFlavorFamily;
import com.stratocloud.provider.aliyun.flavor.AliyunFlavorId;
import com.stratocloud.provider.aliyun.image.AliyunImage;
import com.stratocloud.provider.aliyun.instance.AliyunInstance;
import com.stratocloud.provider.aliyun.instance.command.AliyunInvocation;
import com.stratocloud.provider.aliyun.keypair.AliyunKeyPair;
import com.stratocloud.provider.aliyun.nic.AliyunNic;
import com.stratocloud.provider.aliyun.securitygroup.AliyunSecurityGroup;
import com.stratocloud.provider.aliyun.securitygroup.policy.AliyunSecurityGroupPolicy;
import com.stratocloud.provider.aliyun.securitygroup.policy.AliyunSecurityGroupPolicyId;
import com.stratocloud.provider.aliyun.zone.AliyunZone;

import java.util.List;
import java.util.Optional;

public interface AliyunComputeService {
    List<AliyunZone> describeZones();

    List<DescribeAvailableResourceResponseBody.DescribeAvailableResourceResponseBodyAvailableZonesAvailableZone> describeAvailableResources(DescribeAvailableResourceRequest describeAvailableResourceRequest);

    Optional<AliyunZone> describeZone(String zoneId);

    List<AliyunFlavor> describeFlavors(DescribeInstanceTypesRequest request);

    Optional<AliyunFlavor> describeFlavor(AliyunFlavorId flavorId);

    List<AliyunFlavorFamily> describeInstanceFamilies();

    DescribePriceResponseBody describePrice(DescribePriceRequest request);

    Optional<AliyunSecurityGroup> describeSecurityGroup(String securityGroupId);

    List<AliyunSecurityGroup> describeSecurityGroups(DescribeSecurityGroupsRequest request);

    Optional<AliyunSecurityGroupPolicy> describeSecurityGroupPolicy(AliyunSecurityGroupPolicyId policyId);

    List<AliyunSecurityGroupPolicy> describeSecurityGroupPolicies();

    String createSecurityGroup(CreateSecurityGroupRequest request);

    void deleteSecurityGroup(String securityGroupId);

    String addSecurityGroupPolicy(AuthorizeSecurityGroupRequest request);

    String addSecurityGroupPolicy(AuthorizeSecurityGroupEgressRequest request);

    void deleteIngressPolicy(AliyunSecurityGroupPolicyId policyId);


    void deleteEgressPolicy(AliyunSecurityGroupPolicyId policyId);

    Optional<AliyunNic> describeNic(String nicId);

    List<AliyunNic> describeNics(DescribeNetworkInterfacesRequest request);

    void modifyNic(ModifyNetworkInterfaceAttributeRequest request);

    void assignPrivateIps(AssignPrivateIpAddressesRequest request);

    String createNic(CreateNetworkInterfaceRequest request);

    void deleteNic(String nicId);

    void joinSecurityGroup(JoinSecurityGroupRequest request);

    void leaveSecurityGroup(LeaveSecurityGroupRequest request);

    Optional<AliyunInstance> describeInstance(String instanceId);

    List<AliyunInstance> describeInstances(DescribeInstancesRequest request);

    Optional<AliyunDisk> describeDisk(String diskId);

    List<AliyunDisk> describeDisks(DescribeDisksRequest request);

    DescribeRenewalPriceResponseBody describeRenewalPrice(DescribeRenewalPriceRequest request);

    void attachNic(String instanceId, String nicId);

    void detachNic(String instanceId, String nicId);

    Optional<AliyunImage> describeImage(String imageId);

    List<AliyunImage> describeImages(DescribeImagesRequest request);


    void modifyDisk(ModifyDiskAttributeRequest modifyRequest);

    String createDisk(CreateDiskRequest request);

    void deleteDisk(String diskId);

    void resizeDisk(ResizeDiskRequest request);

    void attachDisk(String instanceId, String diskId);

    void detachDisk(String instanceId, String diskId);

    String runInstance(RunInstancesRequest request);

    Optional<AliyunNic> describePrimaryNicByInstanceId(String instanceId);

    Optional<AliyunDisk> describeSystemDiskByInstanceId(String instanceId);

    void deleteInstance(String instanceId);

    void restartInstance(String instanceId, boolean forceStop);

    void startInstance(String instanceId);

    void stopInstance(String instanceId, boolean forceStop);

    DescribeInstanceModificationPriceResponseBody describeInstanceModificationPrice(DescribeInstanceModificationPriceRequest request);

    void modifyInstanceSpec(ModifyInstanceSpecRequest request);

    void modifyPrepayInstanceSpec(ModifyPrepayInstanceSpecRequest request);

    void replaceSystemDisk(ReplaceSystemDiskRequest request);

    Optional<AliyunKeyPair> describeKeyPair(String keyPairName);

    List<AliyunKeyPair> describeKeyPairs(DescribeKeyPairsRequest request);

    void attachKeyPair(AttachKeyPairRequest request);

    void detachKeyPair(DetachKeyPairRequest request);

    String describeInstanceConsoleUrl(String instanceId);

    CreateKeyPairResponseBody createKeyPair(CreateKeyPairRequest request);

    void deleteKeyPair(String keyPairName);

    void modifyInstanceChargeType(ModifyInstanceChargeTypeRequest request);

    void modifyInstance(ModifyInstanceAttributeRequest request);


    DescribeEniMonitorDataResponseBody describeNicMonitorData(DescribeEniMonitorDataRequest request);

    DescribeDiskMonitorDataResponseBody describeDiskMonitorData(DescribeDiskMonitorDataRequest request);

    List<AliyunInvocation> describeInvocations(DescribeInvocationsRequest request);

    Optional<AliyunInvocation> describeInvocation(String invokeId);

    AliyunInvocation runCommand(RunCommandRequest request);
}
