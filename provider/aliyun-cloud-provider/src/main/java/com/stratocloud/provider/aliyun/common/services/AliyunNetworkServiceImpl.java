package com.stratocloud.provider.aliyun.common.services;

import com.aliyun.teaopenapi.models.Config;
import com.aliyun.vpc20160428.Client;
import com.aliyun.vpc20160428.models.*;
import com.stratocloud.cache.CacheService;
import com.stratocloud.exceptions.ExternalAccountInvalidException;
import com.stratocloud.provider.aliyun.eip.AliyunBandwidthPackage;
import com.stratocloud.provider.aliyun.eip.AliyunEip;
import com.stratocloud.provider.aliyun.subnet.AliyunSubnet;
import com.stratocloud.provider.aliyun.vpc.AliyunVpc;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class AliyunNetworkServiceImpl extends AliyunAbstractService implements AliyunNetworkService {

    public AliyunNetworkServiceImpl(CacheService cacheService, Config config) {
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
    public Optional<AliyunVpc> describeVpc(String vpcId){
        DescribeVpcsRequest request = new DescribeVpcsRequest();
        request.setVpcId(vpcId);
        return describeVpcs(request).stream().findAny();
    }

    @Override
    public List<AliyunVpc> describeVpcs(DescribeVpcsRequest request){
        request.setRegionId(config.getRegionId());
        return queryAll(
                () -> buildClient().describeVpcs(request),
                resp -> resp.getBody().getVpcs().getVpc(),
                resp -> resp.getBody().getTotalCount(),
                request::setPageNumber,
                request::setPageSize
        ).stream().map(AliyunVpc::new).toList();
    }


    @Override
    public String createVpc(CreateVpcRequest request) {
        request.setRegionId(config.getRegionId());

        if(request.getDryRun() == null || !request.getDryRun()) {
            CreateVpcResponseBody responseBody = tryInvoke(() -> buildClient().createVpc(request)).getBody();
            log.info("Aliyun create vpc request sent. RequestId={}. VpcId={}",
                    responseBody.getRequestId(), responseBody.getVpcId());
            return responseBody.getVpcId();
        }else {
            tryInvoke(() -> buildClient().createVpc(request));
            return null;
        }
    }

    @Override
    public void deleteVpc(String vpcId, boolean dryRun) {
        DeleteVpcRequest request = new DeleteVpcRequest();
        request.setVpcId(vpcId);
        request.setRegionId(config.getRegionId());
        request.setDryRun(dryRun);



        if(!dryRun) {
            DeleteVpcResponseBody responseBody = tryInvoke(() -> buildClient().deleteVpc(request)).getBody();
            log.info("Aliyun delete vpc request sent. RequestId={}. VpcId={}",
                    responseBody.getRequestId(), vpcId);
        }else {
            tryInvoke(() -> buildClient().deleteVpc(request));
        }
    }

    @Override
    public Optional<AliyunSubnet> describeSubnet(String subnetId) {
        DescribeVSwitchesRequest request = new DescribeVSwitchesRequest();
        request.setRegionId(config.getRegionId());
        request.setVSwitchId(subnetId);
        return describeSubnets(request).stream().findAny();
    }

    @Override
    public List<AliyunSubnet> describeSubnets(DescribeVSwitchesRequest request) {
        request.setRegionId(config.getRegionId());
        return queryAll(
                () -> buildClient().describeVSwitches(request),
                resp -> resp.getBody().getVSwitches().getVSwitch(),
                resp -> resp.getBody().getTotalCount(),
                request::setPageNumber,
                request::setPageSize
        ).stream().map(AliyunSubnet::new).toList();
    }


    @Override
    public String createSubnet(CreateVSwitchRequest request) {
        request.setRegionId(config.getRegionId());
        CreateVSwitchResponseBody responseBody = tryInvoke(() -> buildClient().createVSwitch(request)).getBody();

        log.info("Aliyun create v-switch request sent. RequestId={}. VSwitchId={}",
                responseBody.getRequestId(), responseBody.getVSwitchId());

        return responseBody.getVSwitchId();
    }


    @Override
    public void deleteSubnet(String vSwitchId) {
        DeleteVSwitchRequest request = new DeleteVSwitchRequest();
        request.setRegionId(config.getRegionId());
        request.setVSwitchId(vSwitchId);

        DeleteVSwitchResponseBody responseBody = tryInvoke(() -> buildClient().deleteVSwitch(request)).getBody();

        log.info("Aliyun delete v-switch request sent. RequestId={}. VSwitchId={}",
                responseBody.getRequestId(), vSwitchId);
    }

    @Override
    public List<AliyunEip> describeEips(DescribeEipAddressesRequest request) {
        request.setRegionId(config.getRegionId());
        return queryAll(
                () -> buildClient().describeEipAddresses(request),
                resp -> resp.getBody().getEipAddresses().getEipAddress(),
                resp -> resp.getBody().getTotalCount(),
                request::setPageNumber,
                request::setPageSize
        ).stream().map(AliyunEip::new).toList();
    }

    @Override
    public Optional<AliyunEip> describeEip(String eipId) {
        DescribeEipAddressesRequest request = new DescribeEipAddressesRequest();
        request.setAllocationId(eipId);
        return describeEips(request).stream().findAny();
    }

    @Override
    public List<AliyunBandwidthPackage> describeBandwidthPackages(DescribeCommonBandwidthPackagesRequest request) {
        request.setRegionId(config.getRegionId());
        return queryAll(
                () -> buildClient().describeCommonBandwidthPackages(request),
                resp -> resp.getBody().getCommonBandwidthPackages().getCommonBandwidthPackage(),
                resp -> resp.getBody().getTotalCount(),
                request::setPageNumber,
                request::setPageSize
        ).stream().map(AliyunBandwidthPackage::new).toList();
    }

    @Override
    public Optional<AliyunBandwidthPackage> describeBandwidthPackage(String bwpId) {
        DescribeCommonBandwidthPackagesRequest request = new DescribeCommonBandwidthPackagesRequest();
        request.setBandwidthPackageId(bwpId);
        return describeBandwidthPackages(request).stream().findAny();
    }

    @Override
    public String createBandwidthPackage(CreateCommonBandwidthPackageRequest request) {
        request.setRegionId(config.getRegionId());
        CreateCommonBandwidthPackageResponseBody responseBody = tryInvoke(
                () -> buildClient().createCommonBandwidthPackage(request)
        ).getBody();

        log.info("Aliyun create bandwidth package request sent. RequestId={}. BwpId={}.",
                responseBody.getRequestId(), responseBody.getBandwidthPackageId());

        return responseBody.getBandwidthPackageId();
    }

    @Override
    public void deleteBandwidthPackage(String bwpId) {
        DeleteCommonBandwidthPackageRequest request = new DeleteCommonBandwidthPackageRequest();
        request.setBandwidthPackageId(bwpId);
        request.setRegionId(config.getRegionId());

        var responseBody = tryInvoke(() -> buildClient().deleteCommonBandwidthPackage(request)).getBody();

        log.info("Aliyun delete bandwidth package request sent. RequestId={}. BwpId={}.",
                responseBody.getRequestId(), bwpId);
    }

    @Override
    public String createEip(AllocateEipAddressRequest request) {
        request.setRegionId(config.getRegionId());

        var responseBody = tryInvoke(() -> buildClient().allocateEipAddress(request)).getBody();

        log.info("Aliyun create eip request sent. RequestId={}. EipId={}. Eip={}.",
                responseBody.getRequestId(), responseBody.getAllocationId(), responseBody.getEipAddress());

        return responseBody.getAllocationId();
    }

    @Override
    public void deleteEip(String eipId) {
        ReleaseEipAddressRequest request = new ReleaseEipAddressRequest();
        request.setRegionId(config.getRegionId());
        request.setAllocationId(eipId);

        ReleaseEipAddressResponseBody responseBody = tryInvoke(
                () -> buildClient().releaseEipAddress(request)
        ).getBody();

        log.info("Aliyun release eip request sent. RequestId={}. EipId={}.",
                responseBody.getRequestId(), eipId);
    }


    @Override
    public void addBandwidthPackageIp(String bwpId, String eipId) {
        AddCommonBandwidthPackageIpRequest request = new AddCommonBandwidthPackageIpRequest();
        request.setBandwidthPackageId(bwpId);
        request.setIpInstanceId(eipId);
        request.setRegionId(config.getRegionId());

        AddCommonBandwidthPackageIpResponseBody responseBody = tryInvoke(
                () -> buildClient().addCommonBandwidthPackageIp(request)
        ).getBody();

        log.info("Aliyun add bandwidth package ip request sent. RequestId={}. BwpId={}. EipId={}.",
                responseBody.getRequestId(), bwpId, eipId);
    }


    @Override
    public void removeBandwidthPackageIp(String bwpId, String eipId) {
        RemoveCommonBandwidthPackageIpRequest request = new RemoveCommonBandwidthPackageIpRequest();
        request.setBandwidthPackageId(bwpId);
        request.setIpInstanceId(eipId);
        request.setRegionId(config.getRegionId());

        RemoveCommonBandwidthPackageIpResponseBody responseBody = tryInvoke(
                () -> buildClient().removeCommonBandwidthPackageIp(request)
        ).getBody();

        log.info("Aliyun remove bandwidth package ip request sent. RequestId={}. BwpId={}. EipId={}.",
                responseBody.getRequestId(), bwpId, eipId);
    }

    @Override
    public void associateEip(AssociateEipAddressRequest request) {
        request.setRegionId(config.getRegionId());

        AssociateEipAddressResponseBody responseBody = tryInvoke(
                () -> buildClient().associateEipAddress(request)
        ).getBody();

        log.info("Aliyun associate eip request sent. RequestId={}. EipId={}.",
                responseBody.getRequestId(), request.getAllocationId());
    }

    @Override
    public void disassociateEip(String eipId) {
        UnassociateEipAddressRequest request = new UnassociateEipAddressRequest();
        request.setAllocationId(eipId);
        request.setRegionId(config.getRegionId());

        UnassociateEipAddressResponseBody responseBody = tryInvoke(
                () -> buildClient().unassociateEipAddress(request)
        ).getBody();

        log.info("Aliyun disassociate eip request sent. RequestId={}. EipId={}.",
                responseBody.getRequestId(), eipId);
    }
}
