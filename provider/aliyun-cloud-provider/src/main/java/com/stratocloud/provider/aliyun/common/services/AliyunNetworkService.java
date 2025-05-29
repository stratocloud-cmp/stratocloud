package com.stratocloud.provider.aliyun.common.services;

import com.aliyun.vpc20160428.models.*;
import com.stratocloud.provider.aliyun.eip.AliyunBandwidthPackage;
import com.stratocloud.provider.aliyun.eip.AliyunEip;
import com.stratocloud.provider.aliyun.subnet.AliyunSubnet;
import com.stratocloud.provider.aliyun.vpc.AliyunVpc;

import java.util.List;
import java.util.Optional;

public interface AliyunNetworkService {
    Optional<AliyunVpc> describeVpc(String vpcId);

    List<AliyunVpc> describeVpcs(DescribeVpcsRequest request);

    String createVpc(CreateVpcRequest request);

    void deleteVpc(String vpcId, boolean dryRun);

    Optional<AliyunSubnet> describeSubnet(String subnetId);

    List<AliyunSubnet> describeSubnets(DescribeVSwitchesRequest request);

    String createSubnet(CreateVSwitchRequest request);

    void deleteSubnet(String vSwitchId);

    List<AliyunEip> describeEips(DescribeEipAddressesRequest request);

    List<AliyunEip> describeEipsByAssociatedInstanceId(String instanceId);

    Optional<AliyunEip> describeEip(String eipId);

    Optional<AliyunBandwidthPackage> describeBandwidthPackage(String bwpId);

    List<AliyunBandwidthPackage> describeBandwidthPackages(DescribeCommonBandwidthPackagesRequest request);

    String createBandwidthPackage(CreateCommonBandwidthPackageRequest request);

    void deleteBandwidthPackage(String bwpId);

    String createEip(AllocateEipAddressRequest request);

    void deleteEip(String eipId);


    void addBandwidthPackageIp(String bwpId, String eipId);

    void removeBandwidthPackageIp(String bwpId, String eipId);

    void associateEip(AssociateEipAddressRequest request);

    void disassociateEip(String eipId);
}
