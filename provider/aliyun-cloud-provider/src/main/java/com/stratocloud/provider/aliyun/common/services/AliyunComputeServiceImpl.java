package com.stratocloud.provider.aliyun.common.services;

import com.aliyun.ecs20140526.Client;
import com.aliyun.ecs20140526.models.*;
import com.aliyun.teaopenapi.models.Config;
import com.stratocloud.cache.CacheService;
import com.stratocloud.cache.CacheUtil;
import com.stratocloud.exceptions.ExternalAccountInvalidException;
import com.stratocloud.exceptions.StratoException;
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
import com.stratocloud.provider.aliyun.snapshot.AliyunSnapshot;
import com.stratocloud.provider.aliyun.zone.AliyunZone;
import com.stratocloud.resource.ResourceState;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.stratocloud.utils.concurrent.SleepUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@SuppressWarnings("Convert2MethodRef")
public class AliyunComputeServiceImpl extends AliyunAbstractService implements AliyunComputeService {

    public AliyunComputeServiceImpl(CacheService cacheService, Config config) {
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
    public List<AliyunZone> describeZones(){
        Supplier<List<AliyunZone>> queryFunction = this::doDescribeZones;
        return CacheUtil.queryWithCache(
                cacheService,
                buildCacheKey("Zones"),
                300L,
                queryFunction,
                new ArrayList<>()
        );
    }

    private List<AliyunZone> doDescribeZones() {
        DescribeZonesRequest describeZonesRequest = new DescribeZonesRequest();
        describeZonesRequest.setRegionId(config.getRegionId());

        var zones = tryInvoke(
                () -> buildClient().describeZones(describeZonesRequest)
        ).getBody().getZones().getZone();

        DescribeAvailableResourceRequest describeAvailableResourceRequest = new DescribeAvailableResourceRequest();
        describeAvailableResourceRequest.setDestinationResource("Zone");

        var availabilities = describeAvailableResources(describeAvailableResourceRequest).stream().collect(
                Collectors.toMap(
                        a -> a.getZoneId(),
                        a -> a
                )
        );

        return new ArrayList<>(
                zones.stream().map(z -> new AliyunZone(z, availabilities.get(z.getZoneId()))).toList()
        );
    }

    @Override
    public List<DescribeAvailableResourceResponseBody.DescribeAvailableResourceResponseBodyAvailableZonesAvailableZone> describeAvailableResources(DescribeAvailableResourceRequest describeAvailableResourceRequest) {
        return CacheUtil.queryWithCache(
                cacheService,
                buildCacheKey("AvailableResources", describeAvailableResourceRequest),
                600,
                () -> doDescribeAvailableResources(describeAvailableResourceRequest),
                new ArrayList<>()
        );
    }

    private List<DescribeAvailableResourceResponseBody.DescribeAvailableResourceResponseBodyAvailableZonesAvailableZone> doDescribeAvailableResources(DescribeAvailableResourceRequest describeAvailableResourceRequest) {
        describeAvailableResourceRequest.setRegionId(config.getRegionId());

        var zones = tryInvoke(
                () -> buildClient().describeAvailableResource(describeAvailableResourceRequest)
        ).getBody().getAvailableZones();

        if(zones == null)
            return new ArrayList<>();

        var result = zones.getAvailableZone();

        if(Utils.isEmpty(result))
            return new ArrayList<>();
        else
            return new ArrayList<>(result);
    }

    private List<DescribeInstanceTypesResponseBody.DescribeInstanceTypesResponseBodyInstanceTypesInstanceType> describeInstanceTypes(DescribeInstanceTypesRequest request){
        return queryAllByToken(
                () -> buildClient().describeInstanceTypes(request),
                resp -> resp.getBody().getInstanceTypes().getInstanceType(),
                resp -> resp.getBody().getNextToken(),
                request::setNextToken
        );
    }

    @Override
    public Optional<AliyunZone> describeZone(String zoneId){
        return describeZones().stream().filter(
                z -> Objects.equals(z.getZoneId(), zoneId)
        ).findAny();
    }



    @Override
    public List<AliyunFlavor> describeFlavors(DescribeInstanceTypesRequest request) {
        Supplier<List<AliyunFlavor>> queryFunction = () -> doDescribeFlavors(request);
        return CacheUtil.queryWithCache(
                cacheService,
                buildCacheKey("InstanceTypes", request),
                300L,
                queryFunction,
                new ArrayList<>()
        );
    }

    private List<AliyunFlavor> doDescribeFlavors(DescribeInstanceTypesRequest request) {
        String instanceTypeResourceType = "InstanceType";

        DescribeAvailableResourceRequest describeAvailableResourceRequest = new DescribeAvailableResourceRequest();

        describeAvailableResourceRequest.setDestinationResource(instanceTypeResourceType);

        List<String> instanceTypeIds = request.getInstanceTypes();
        if(Utils.isNotEmpty(instanceTypeIds) && instanceTypeIds.size() == 1)
            describeAvailableResourceRequest.setInstanceType(instanceTypeIds.get(0));

        var zones = describeAvailableResources(describeAvailableResourceRequest);

        var instanceTypes = describeInstanceTypes(request);

        var instanceTypeMap = instanceTypes.stream().collect(
                Collectors.toMap(
                        it -> it.getInstanceTypeId(),
                        it -> it
                )
        );

        List<AliyunFlavor> result = new ArrayList<>();

        if(Utils.isEmpty(zones) || Utils.isEmpty(instanceTypes))
            return result;

        for (var zone : zones) {
            var availableResources = zone.getAvailableResources().getAvailableResource();
            if(Utils.isEmpty(availableResources))
                continue;
            for (var availableResource : availableResources) {
                if(!Objects.equals(instanceTypeResourceType, availableResource.getType()))
                    continue;

                var supportedResources = availableResource.getSupportedResources().getSupportedResource();

                if(Utils.isEmpty(supportedResources))
                    continue;

                for (var supportedResource : supportedResources) {
                    if(!instanceTypeMap.containsKey(supportedResource.getValue()))
                        continue;

                    result.add(new AliyunFlavor(
                            new AliyunFlavorId(zone.getZoneId(), supportedResource.getValue()),
                            instanceTypeMap.get(supportedResource.getValue()),
                            convertFlavorStatus(supportedResource.getStatus())
                    ));
                }
            }
        }

        return result;
    }

    private ResourceState convertFlavorStatus(String status) {
        return switch (status) {
            case "Available" -> ResourceState.AVAILABLE;
            case "SoldOut" -> ResourceState.SOLD_OUT;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public Optional<AliyunFlavor> describeFlavor(AliyunFlavorId flavorId) {
        DescribeInstanceTypesRequest request = new DescribeInstanceTypesRequest();
        request.setInstanceTypes(List.of(flavorId.instanceTypeId()));
        return describeFlavors(request).stream().filter(
                aliyunFlavor -> Objects.equals(flavorId, aliyunFlavor.flavorId())
        ).findAny();
    }


    @Override
    public List<AliyunFlavorFamily> describeInstanceFamilies() {
        Supplier<List<AliyunFlavorFamily>> queryFunction = this::doDescribeInstanceFamilies;
        return CacheUtil.queryWithCache(
                cacheService,
                buildCacheKey("InstanceTypeFamilies"),
                300L,
                queryFunction,
                new ArrayList<>()
        );
    }

    private List<AliyunFlavorFamily> doDescribeInstanceFamilies() {
        DescribeInstanceTypeFamiliesRequest request = new DescribeInstanceTypeFamiliesRequest();
        request.setRegionId(config.getRegionId());
        return new ArrayList<>(
                tryInvoke(
                        () -> buildClient().describeInstanceTypeFamilies(request)
                ).getBody().getInstanceTypeFamilies().getInstanceTypeFamily().stream().map(
                        family -> new AliyunFlavorFamily(family)
                ).toList()
        );
    }


    @Override
    public DescribePriceResponseBody describePrice(DescribePriceRequest request) {
        Supplier<DescribePriceResponseBody> queryFunction = () -> doDescribePrice(request);
        return CacheUtil.queryWithCache(
                cacheService,
                buildCacheKey("Price", request),
                300L,
                queryFunction,
                new DescribePriceResponseBody()
        );
    }

    private DescribePriceResponseBody doDescribePrice(DescribePriceRequest request) {
        request.setRegionId(config.getRegionId());
        return tryInvoke(() -> buildClient().describePrice(request)).getBody();
    }

    @Override
    public Optional<AliyunSecurityGroup> describeSecurityGroup(String securityGroupId) {
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();
        request.setSecurityGroupId(securityGroupId);
        return describeSecurityGroups(request).stream().findAny();
    }

    @Override
    public List<AliyunSecurityGroup> describeSecurityGroups(DescribeSecurityGroupsRequest request) {
        request.setRegionId(config.getRegionId());
        return queryAllByToken(
                () -> buildClient().describeSecurityGroups(request),
                resp -> resp.getBody().getSecurityGroups().getSecurityGroup(),
                resp -> resp.getBody().getNextToken(),
                nextToken -> request.setNextToken(nextToken)
        ).stream().map(
                s -> new AliyunSecurityGroup(s)
        ).toList();
    }

    @Override
    public Optional<AliyunSecurityGroupPolicy> describeSecurityGroupPolicy(AliyunSecurityGroupPolicyId policyId) {
        return describeSecurityGroupPolicies(policyId.securityGroupId()).stream().filter(
                policy -> Objects.equals(policyId, policy.policyId())
        ).findAny();
    }


    private List<AliyunSecurityGroupPolicy> describeSecurityGroupPolicies(String securityGroupId){
        DescribeSecurityGroupAttributeRequest request = new DescribeSecurityGroupAttributeRequest();
        request.setSecurityGroupId(securityGroupId);
        return describeSecurityGroupPolicies(request);
    }

    private List<AliyunSecurityGroupPolicy> describeSecurityGroupPolicies(DescribeSecurityGroupAttributeRequest request) {
        request.setRegionId(config.getRegionId());
        return queryAllByToken(
                () -> buildClient().describeSecurityGroupAttribute(request),
                resp -> resp.getBody().getPermissions().getPermission(),
                resp -> resp.getBody().getNextToken(),
                nextToken -> request.setNextToken(nextToken)
        ).stream().map(
                p -> new AliyunSecurityGroupPolicy(
                        new AliyunSecurityGroupPolicyId(request.getSecurityGroupId(), p.getSecurityGroupRuleId()),
                        p
                )
        ).toList();
    }

    @Override
    public List<AliyunSecurityGroupPolicy> describeSecurityGroupPolicies() {
        List<AliyunSecurityGroup> securityGroups = describeSecurityGroups(new DescribeSecurityGroupsRequest());

        if(Utils.isEmpty(securityGroups))
            return List.of();


        List<AliyunSecurityGroupPolicy> result = new ArrayList<>();
        for (AliyunSecurityGroup securityGroup : securityGroups) {
            var policies = describeSecurityGroupPolicies(securityGroup.detail().getSecurityGroupId());

            result.addAll(policies);
        }

        return result;
    }

    @Override
    public String createSecurityGroup(CreateSecurityGroupRequest request) {
        request.setRegionId(config.getRegionId());
        CreateSecurityGroupResponseBody responseBody = tryInvoke(
                () -> buildClient().createSecurityGroup(request)
        ).getBody();

        log.info("Aliyun create security group request sent. RequestId={}. SecurityGroupId={}.",
                responseBody.getRequestId(), responseBody.getSecurityGroupId());

        return responseBody.getSecurityGroupId();
    }

    @Override
    public void deleteSecurityGroup(String securityGroupId) {
        DeleteSecurityGroupRequest request = new DeleteSecurityGroupRequest();
        request.setRegionId(config.getRegionId());
        request.setSecurityGroupId(securityGroupId);

        DeleteSecurityGroupResponseBody responseBody = tryInvoke(
                () -> buildClient().deleteSecurityGroup(request)
        ).getBody();

        log.info("Aliyun delete security group request sent. RequestId={}. SecurityGroupId={}.",
                responseBody.getRequestId(), securityGroupId);
    }


    @Override
    public String addSecurityGroupPolicy(AuthorizeSecurityGroupRequest request) {
        request.setRegionId(config.getRegionId());

        var permissionsList = request.getPermissions();

        if(Utils.isEmpty(permissionsList) || permissionsList.size() > 1)
            throw new StratoException("Add 1 permission only each request.");

        var permissions = permissionsList.get(0);

        String destCidrIp = permissions.getDestCidrIp();
        String ipProtocol = permissions.getIpProtocol();
        String ipv6DestCidrIp = permissions.getIpv6DestCidrIp();
        String ipv6SourceCidrIp = permissions.getIpv6SourceCidrIp();
        String policy = permissions.getPolicy();
        String portRange = permissions.getPortRange();
        String sourceCidrIp = permissions.getSourceCidrIp();
        String sourceGroupId = permissions.getSourceGroupId();
        String sourcePortRange = permissions.getSourcePortRange();
        String sourcePrefixListId = permissions.getSourcePrefixListId();

        var responseBody = tryInvoke(() -> buildClient().authorizeSecurityGroup(request)).getBody();

        log.info("Aliyun ingress policy added. RequestId={}.", responseBody.getRequestId());

        var describeSecurityGroupAttributeRequest = new DescribeSecurityGroupAttributeRequest();

        describeSecurityGroupAttributeRequest.setDirection("ingress");
        describeSecurityGroupAttributeRequest.setSecurityGroupId(request.getSecurityGroupId());


        List<AliyunSecurityGroupPolicy> policies = describeSecurityGroupPolicies(describeSecurityGroupAttributeRequest);

        List<AliyunSecurityGroupPolicy> matchedPolicies = policies.stream().filter(
                p -> {
                    if (Utils.isNotBlank(destCidrIp) && !destCidrIp.equalsIgnoreCase(p.detail().getDestCidrIp()))
                        return false;
                    if (Utils.isNotBlank(ipProtocol) && !ipProtocol.equalsIgnoreCase(p.detail().getIpProtocol()))
                        return false;
                    if (Utils.isNotBlank(ipv6DestCidrIp) && !ipv6DestCidrIp.equalsIgnoreCase(p.detail().getIpv6DestCidrIp()))
                        return false;

                    if (Utils.isNotBlank(ipv6SourceCidrIp) && !ipv6SourceCidrIp.equalsIgnoreCase(p.detail().getIpv6SourceCidrIp()))
                        return false;
                    if (Utils.isNotBlank(policy) && !policy.equalsIgnoreCase(p.detail().getPolicy()))
                        return false;
                    if (Utils.isNotBlank(portRange) && !portRange.equalsIgnoreCase(p.detail().getPortRange()))
                        return false;
                    if (Utils.isNotBlank(sourceCidrIp) && !sourceCidrIp.equalsIgnoreCase(p.detail().getSourceCidrIp()))
                        return false;
                    if (Utils.isNotBlank(sourceGroupId) && !sourceGroupId.equalsIgnoreCase(p.detail().getSourceGroupId()))
                        return false;
                    if (Utils.isNotBlank(sourcePortRange) && !sourcePortRange.equalsIgnoreCase(p.detail().getSourcePortRange()))
                        return false;
                    if (Utils.isNotBlank(sourcePrefixListId) && !sourcePrefixListId.equalsIgnoreCase(p.detail().getSourcePrefixListId()))
                        return false;

                    log.info("Ingress policy matched. RuleId={}.", p.policyId().ruleId());

                    return true;
                }
        ).toList();

        if(Utils.isEmpty(matchedPolicies))
            throw new StratoException("Ingress policy not found after added.");

        if(matchedPolicies.size()>1)
            throw new StratoException("Multiple ingress policies matched after 1 policy added: %s".formatted(matchedPolicies));

        return matchedPolicies.get(0).policyId().ruleId();
    }

    @Override
    public String addSecurityGroupPolicy(AuthorizeSecurityGroupEgressRequest request) {
        request.setRegionId(config.getRegionId());

        var permissionsList = request.getPermissions();

        if(Utils.isEmpty(permissionsList) || permissionsList.size() > 1)
            throw new StratoException("Add 1 permission only each request.");

        var permissions = permissionsList.get(0);

        String destGroupId = permissions.getDestGroupId();
        String destPrefixListId = permissions.getDestPrefixListId();

        String destCidrIp = permissions.getDestCidrIp();
        String ipProtocol = permissions.getIpProtocol();
        String ipv6DestCidrIp = permissions.getIpv6DestCidrIp();
        String ipv6SourceCidrIp = permissions.getIpv6SourceCidrIp();
        String policy = permissions.getPolicy();
        String portRange = permissions.getPortRange();
        String sourceCidrIp = permissions.getSourceCidrIp();

        String sourcePortRange = permissions.getSourcePortRange();


        var responseBody = tryInvoke(() -> buildClient().authorizeSecurityGroupEgress(request)).getBody();

        log.info("Aliyun egress policy added. RequestId={}.", responseBody.getRequestId());

        var describeSecurityGroupAttributeRequest = new DescribeSecurityGroupAttributeRequest();

        describeSecurityGroupAttributeRequest.setDirection("egress");
        describeSecurityGroupAttributeRequest.setSecurityGroupId(request.getSecurityGroupId());


        List<AliyunSecurityGroupPolicy> policies = describeSecurityGroupPolicies(describeSecurityGroupAttributeRequest);

        List<AliyunSecurityGroupPolicy> matchedPolicies = policies.stream().filter(
                p -> {
                    if (Utils.isNotBlank(destCidrIp) && !destCidrIp.equalsIgnoreCase(p.detail().getDestCidrIp()))
                        return false;
                    if (Utils.isNotBlank(ipProtocol) && !ipProtocol.equalsIgnoreCase(p.detail().getIpProtocol()))
                        return false;
                    if (Utils.isNotBlank(ipv6DestCidrIp) && !ipv6DestCidrIp.equalsIgnoreCase(p.detail().getIpv6DestCidrIp()))
                        return false;

                    if (Utils.isNotBlank(ipv6SourceCidrIp) && !ipv6SourceCidrIp.equalsIgnoreCase(p.detail().getIpv6SourceCidrIp()))
                        return false;
                    if (Utils.isNotBlank(policy) && !policy.equalsIgnoreCase(p.detail().getPolicy()))
                        return false;
                    if (Utils.isNotBlank(portRange) && !portRange.equalsIgnoreCase(p.detail().getPortRange()))
                        return false;
                    if (Utils.isNotBlank(sourceCidrIp) && !sourceCidrIp.equalsIgnoreCase(p.detail().getSourceCidrIp()))
                        return false;
                    if (Utils.isNotBlank(destGroupId) && !destGroupId.equalsIgnoreCase(p.detail().getDestGroupId()))
                        return false;
                    if (Utils.isNotBlank(sourcePortRange) && !sourcePortRange.equalsIgnoreCase(p.detail().getSourcePortRange()))
                        return false;
                    if (Utils.isNotBlank(destPrefixListId) && !destPrefixListId.equalsIgnoreCase(p.detail().getDestPrefixListId()))
                        return false;

                    log.info("Egress policy matched. RuleId={}.", p.policyId().ruleId());

                    return true;
                }
        ).toList();

        if(Utils.isEmpty(matchedPolicies))
            throw new StratoException("Egress policy not found after added.");

        if(matchedPolicies.size()>1)
            throw new StratoException("Multiple egress policies matched after 1 policy added: %s".formatted(matchedPolicies));

        return matchedPolicies.get(0).policyId().ruleId();
    }

    @Override
    public void deleteIngressPolicy(AliyunSecurityGroupPolicyId policyId) {
        RevokeSecurityGroupRequest request = new RevokeSecurityGroupRequest();
        request.setRegionId(config.getRegionId());
        request.setSecurityGroupId(policyId.securityGroupId());
        request.setSecurityGroupRuleId(List.of(policyId.ruleId()));
        var responseBody = tryInvoke(() -> buildClient().revokeSecurityGroup(request)).getBody();
        log.info("Aliyun delete ingress rule request sent. RequestId={}. RuleId={}.",
                responseBody.getRequestId(), policyId.ruleId());
    }

    @Override
    public void deleteEgressPolicy(AliyunSecurityGroupPolicyId policyId) {
        RevokeSecurityGroupEgressRequest request = new RevokeSecurityGroupEgressRequest();
        request.setRegionId(config.getRegionId());
        request.setSecurityGroupId(policyId.securityGroupId());
        request.setSecurityGroupRuleId(List.of(policyId.ruleId()));
        var responseBody = tryInvoke(() -> buildClient().revokeSecurityGroupEgress(request)).getBody();
        log.info("Aliyun delete egress rule request sent. RequestId={}. RuleId={}.",
                responseBody.getRequestId(), policyId.ruleId());
    }


    @Override
    public Optional<AliyunNic> describeNic(String nicId) {
        DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();
        request.setNetworkInterfaceId(List.of(nicId));
        return describeNics(request).stream().findAny();
    }

    @Override
    public List<AliyunNic> describeNics(DescribeNetworkInterfacesRequest request) {
        request.setRegionId(config.getRegionId());
        return queryAllByToken(
                () -> buildClient().describeNetworkInterfaces(request),
                resp -> resp.getBody().getNetworkInterfaceSets().getNetworkInterfaceSet(),
                resp -> resp.getBody().getNextToken(),
                nextToken -> request.setNextToken(nextToken)
        ).stream().map(
                nic -> new AliyunNic(nic)
        ).toList();
    }

    @Override
    public void modifyNic(ModifyNetworkInterfaceAttributeRequest request) {
        request.setRegionId(config.getRegionId());

        var responseBody = tryInvoke(() -> buildClient().modifyNetworkInterfaceAttribute(request)).getBody();

        log.info("Aliyun modify nic attribute request sent. RequestId={}. NicId={}.",
                responseBody.getRequestId(), request.getNetworkInterfaceId());
    }


    @Override
    public void assignPrivateIps(AssignPrivateIpAddressesRequest request) {
        request.setRegionId(config.getRegionId());
        AssignPrivateIpAddressesResponseBody responseBody = tryInvoke(
                () -> buildClient().assignPrivateIpAddresses(request)
        ).getBody();

        log.info("Aliyun assign nic private ips request sent. RequestId={}. NicId={}.",
                responseBody.getRequestId(), request.getNetworkInterfaceId());
    }


    @Override
    public String createNic(CreateNetworkInterfaceRequest request) {
        request.setRegionId(config.getRegionId());

        CreateNetworkInterfaceResponseBody responseBody = tryInvoke(
                () -> buildClient().createNetworkInterface(request)
        ).getBody();

        log.info("Aliyun create nic request sent. RequestId={}. NicId={}.",
                responseBody.getRequestId(), responseBody.getNetworkInterfaceId());

        return responseBody.getNetworkInterfaceId();
    }

    @Override
    public void deleteNic(String nicId) {
        DeleteNetworkInterfaceRequest request = new DeleteNetworkInterfaceRequest();
        request.setRegionId(config.getRegionId());
        request.setNetworkInterfaceId(nicId);

        var responseBody = tryInvoke(() -> buildClient().deleteNetworkInterface(request)).getBody();

        log.info("Aliyun delete nic request sent. RequestId={}. NicId={}.",
                responseBody.getRequestId(), nicId);
    }

    @Override
    public void joinSecurityGroup(JoinSecurityGroupRequest request) {
        request.setRegionId(config.getRegionId());

        var responseBody = tryInvoke(() -> buildClient().joinSecurityGroup(request)).getBody();

        log.info("Aliyun join security group request sent. RequestId={}. SecurityGroupId={}.",
                responseBody.getRequestId(), request.getSecurityGroupId());
    }


    @Override
    public void leaveSecurityGroup(LeaveSecurityGroupRequest request) {
        request.setRegionId(config.getRegionId());

        var responseBody = tryInvoke(() -> buildClient().leaveSecurityGroup(request)).getBody();

        log.info("Aliyun leave security group request sent. RequestId={}. SecurityGroupId={}.",
                responseBody.getRequestId(), request.getSecurityGroupId());
    }

    @Override
    public Optional<AliyunInstance> describeInstance(String instanceId) {
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        request.setInstanceIds(JSON.toJsonString(List.of(instanceId)));
        return describeInstances(request).stream().findAny();
    }

    @Override
    public List<AliyunInstance> describeInstances(DescribeInstancesRequest request) {
        request.setRegionId(config.getRegionId());
        return queryAllByToken(
                () -> buildClient().describeInstances(request),
                resp -> resp.getBody().getInstances().getInstance(),
                resp -> resp.getBody().getNextToken(),
                nextToken -> request.setNextToken(nextToken)
        ).stream().map(
                i -> new AliyunInstance(i)
        ).toList();
    }

    @Override
    public Optional<AliyunDisk> describeDisk(String diskId) {
        DescribeDisksRequest request = new DescribeDisksRequest();
        request.setRegionId(config.getRegionId());
        request.setDiskIds(JSON.toJsonString(List.of(diskId)));
        return describeDisks(request).stream().findAny();
    }

    @Override
    public List<AliyunDisk> describeDisks(DescribeDisksRequest request) {
        request.setRegionId(config.getRegionId());
        return queryAllByToken(
                () -> buildClient().describeDisks(request),
                resp -> resp.getBody().getDisks().getDisk(),
                resp -> resp.getBody().getNextToken(),
                nextToken -> request.setNextToken(nextToken)
        ).stream().map(
                disk -> new AliyunDisk(disk)
        ).toList();
    }


    @Override
    public DescribeRenewalPriceResponseBody describeRenewalPrice(DescribeRenewalPriceRequest request) {
        request.setRegionId(config.getRegionId());
        return tryInvoke(() -> buildClient().describeRenewalPrice(request)).getBody();
    }


    @Override
    public void attachNic(String instanceId, String nicId) {
        AttachNetworkInterfaceRequest request = new AttachNetworkInterfaceRequest();
        request.setRegionId(config.getRegionId());
        request.setInstanceId(instanceId);
        request.setNetworkInterfaceId(nicId);

        var responseBody = tryInvoke(() -> buildClient().attachNetworkInterface(request)).getBody();
        log.info("Aliyun attach nic request sent. RequestId={}. InstanceId={}. NicId={}.",
                responseBody.getRequestId(), instanceId, nicId);
    }


    @Override
    public void detachNic(String instanceId, String nicId) {
        DetachNetworkInterfaceRequest request = new DetachNetworkInterfaceRequest();
        request.setRegionId(config.getRegionId());
        request.setInstanceId(instanceId);
        request.setNetworkInterfaceId(nicId);

        var responseBody = tryInvoke(() -> buildClient().detachNetworkInterface(request)).getBody();
        log.info("Aliyun detach nic request sent. RequestId={}. InstanceId={}. NicId={}.",
                responseBody.getRequestId(), instanceId, nicId);
    }

    @Override
    public Optional<AliyunImage> describeImage(String imageId) {
        DescribeImagesRequest request = new DescribeImagesRequest();
        request.setImageId(imageId);
        return describeImages(request).stream().findAny();
    }

    @Override
    public List<AliyunImage> describeImages(DescribeImagesRequest request) {
        request.setRegionId(config.getRegionId());
        return queryAll(
                () -> buildClient().describeImages(request),
                resp -> resp.getBody().getImages().getImage(),
                resp -> resp.getBody().getTotalCount(),
                pageNumber -> request.setPageNumber(pageNumber),
                pageSize -> request.setPageSize(pageSize)
        ).stream().map(
                image -> new AliyunImage(image)
        ).toList();
    }


    @Override
    public void modifyDisk(ModifyDiskAttributeRequest modifyRequest) {
        modifyRequest.setRegionId(config.getRegionId());

        var responseBody = tryInvoke(() -> buildClient().modifyDiskAttribute(modifyRequest)).getBody();
        log.info("Aliyun modify disk request sent. RequestId={}. DiskId={}.",
                responseBody.getRequestId(), modifyRequest.getDiskId());
    }

    @Override
    public String createDisk(CreateDiskRequest request) {
        request.setRegionId(config.getRegionId());
        CreateDiskResponseBody responseBody = tryInvoke(() -> buildClient().createDisk(request)).getBody();

        log.info("Aliyun create disk request sent. RequestId={}. DiskId={}",
                responseBody.getRequestId(), responseBody.getDiskId());

        return responseBody.getDiskId();
    }

    @Override
    public void deleteDisk(String diskId) {
        DeleteDiskRequest request = new DeleteDiskRequest();
        request.setDiskId(diskId);

        DeleteDiskResponseBody responseBody = tryInvoke(() -> buildClient().deleteDisk(request)).getBody();

        log.info("Aliyun delete disk request sent. RequestId={}. DiskId={}",
                responseBody.getRequestId(), diskId);
    }

    @Override
    public void resizeDisk(ResizeDiskRequest request) {
        ResizeDiskResponseBody responseBody = tryInvoke(() -> buildClient().resizeDisk(request)).getBody();

        log.info("Aliyun resize disk request sent. RequestId={}. DiskId={}",
                responseBody.getRequestId(), request.getDiskId());
    }


    @Override
    public void attachDisk(String instanceId, String diskId) {
        AttachDiskRequest request = new AttachDiskRequest();
        request.setInstanceId(instanceId);
        request.setDiskId(diskId);

        AttachDiskResponseBody responseBody = tryInvoke(() -> buildClient().attachDisk(request)).getBody();

        log.info("Aliyun attach disk request sent. RequestId={}. DiskId={}.",
                responseBody.getRequestId(), diskId);
    }

    @Override
    public void detachDisk(String instanceId, String diskId) {
        DetachDiskRequest request = new DetachDiskRequest();
        request.setInstanceId(instanceId);
        request.setDiskId(diskId);
        request.setDeleteWithInstance(false);

        DetachDiskResponseBody responseBody = tryInvoke(() -> buildClient().detachDisk(request)).getBody();
        log.info("Aliyun detach disk request sent. RequestId={}. DiskId={}.",
                responseBody.getRequestId(), diskId);
    }


    @Override
    public String runInstance(RunInstancesRequest request) {
        if(request.getAmount()!=null && request.getAmount()>1)
            throw new StratoException("Do not run multiple instances via this api.");

        request.setRegionId(config.getRegionId());

        if(request.getDryRun() == null || !request.getDryRun()){
            RunInstancesResponseBody responseBody = tryInvoke(() -> buildClient().runInstances(request)).getBody();

            String instanceId = responseBody.getInstanceIdSets().getInstanceIdSet().get(0);

            log.info("Aliyun run instance request sent. RequestId={}. InstanceId={}.",
                    responseBody.getRequestId(), instanceId);

            return instanceId;
        }else {
            tryInvoke(() -> buildClient().runInstances(request));
            return null;
        }
    }


    @Override
    public Optional<AliyunDisk> describeSystemDiskByInstanceId(String instanceId) {
        DescribeDisksRequest request = new DescribeDisksRequest();
        request.setRegionId(config.getRegionId());
        request.setInstanceId(instanceId);
        request.setDiskType("system");

        return describeDisks(request).stream().findAny();
    }

    @Override
    public Optional<AliyunNic> describePrimaryNicByInstanceId(String instanceId) {
        DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();
        request.setRegionId(config.getRegionId());
        request.setInstanceId(instanceId);

        return describeNics(request).stream().filter(
                nic -> nic.isPrimaryNic()
        ).findAny();
    }

    @Override
    public void deleteInstance(String instanceId) {
        DeleteInstanceRequest request = new DeleteInstanceRequest();
        request.setInstanceId(instanceId);

        DeleteInstanceResponseBody responseBody = tryInvoke(() -> buildClient().deleteInstance(request)).getBody();

        log.info("Aliyun delete instance request sent. RequestId={}. InstanceId={}.",
                responseBody.getRequestId(), instanceId);
    }

    @Override
    public void restartInstance(String instanceId, boolean forceStop) {
        RebootInstanceRequest request = new RebootInstanceRequest();
        request.setInstanceId(instanceId);
        request.setForceStop(forceStop);

        RebootInstanceResponseBody responseBody = tryInvoke(() -> buildClient().rebootInstance(request)).getBody();

        log.info("Aliyun reboot instance request sent. RequestId={}. InstanceId={}.",
                responseBody.getRequestId(), instanceId);
    }


    @Override
    public void startInstance(String instanceId) {
        StartInstanceRequest request = new StartInstanceRequest();
        request.setInstanceId(instanceId);

        StartInstanceResponseBody responseBody = tryInvoke(() -> buildClient().startInstance(request)).getBody();

        log.info("Aliyun start instance request sent. RequestId={}. InstanceId={}.",
                responseBody.getRequestId(), instanceId);
    }

    @Override
    public void stopInstance(String instanceId, boolean forceStop) {
        StopInstanceRequest request = new StopInstanceRequest();
        request.setInstanceId(instanceId);
        request.setForceStop(forceStop);

        StopInstanceResponseBody responseBody = tryInvoke(() -> buildClient().stopInstance(request)).getBody();

        log.info("Aliyun stop instance request sent. RequestId={}. InstanceId={}.",
                responseBody.getRequestId(), instanceId);
    }

    @Override
    public DescribeInstanceModificationPriceResponseBody describeInstanceModificationPrice(DescribeInstanceModificationPriceRequest request) {
        request.setRegionId(config.getRegionId());
        return tryInvoke(() -> buildClient().describeInstanceModificationPrice(request)).getBody();
    }

    @Override
    public void modifyInstanceSpec(ModifyInstanceSpecRequest request) {
        ModifyInstanceSpecResponseBody responseBody = tryInvoke(
                () -> buildClient().modifyInstanceSpec(request)
        ).getBody();

        log.info("Aliyun modify instance spec request sent. RequestId={}. InstanceId={}.",
                responseBody.getRequestId(), request.getInstanceId());
    }

    @Override
    public void modifyPrepayInstanceSpec(ModifyPrepayInstanceSpecRequest request) {
        request.setRegionId(config.getRegionId());
        ModifyPrepayInstanceSpecResponseBody responseBody = tryInvoke(
                () -> buildClient().modifyPrepayInstanceSpec(request)
        ).getBody();

        log.info("Aliyun modify prepay instance spec request sent. RequestId={}. InstanceId={}.",
                responseBody.getRequestId(), request.getInstanceId());
    }

    @Override
    public void replaceSystemDisk(ReplaceSystemDiskRequest request) {
        ReplaceSystemDiskResponseBody responseBody = tryInvoke(
                () -> buildClient().replaceSystemDisk(request)
        ).getBody();

        log.info("Aliyun replace system disk request sent. RequestId={}. InstanceId={}.",
                responseBody.getRequestId(), request.getInstanceId());
    }

    @Override
    public List<AliyunKeyPair> describeKeyPairs(DescribeKeyPairsRequest request) {
        request.setRegionId(config.getRegionId());
        return queryAll(
                () -> buildClient().describeKeyPairs(request),
                resp -> resp.getBody().getKeyPairs().getKeyPair(),
                resp -> resp.getBody().getTotalCount(),
                pageNumber -> request.setPageNumber(pageNumber),
                pageSize -> request.setPageSize(pageSize)
        ).stream().map(k -> new AliyunKeyPair(k)).toList();
    }

    @Override
    public Optional<AliyunKeyPair> describeKeyPair(String keyPairName) {
        DescribeKeyPairsRequest request = new DescribeKeyPairsRequest();
        request.setKeyPairName(keyPairName);
        return describeKeyPairs(request).stream().findAny();
    }

    @Override
    public void attachKeyPair(AttachKeyPairRequest request) {
        request.setRegionId(config.getRegionId());

        AttachKeyPairResponseBody responseBody = tryInvoke(() -> buildClient().attachKeyPair(request)).getBody();

        log.info("Aliyun attach id pair request sent. RequestId={}. InstanceId={}.",
                responseBody.getRequestId(), request.getInstanceIds());
    }

    @Override
    public void detachKeyPair(DetachKeyPairRequest request) {
        request.setRegionId(config.getRegionId());

        DetachKeyPairResponseBody responseBody = tryInvoke(() -> buildClient().detachKeyPair(request)).getBody();

        log.info("Aliyun detach id pair request sent. RequestId={}. InstanceId={}.",
                responseBody.getRequestId(), request.getInstanceIds());
    }


    @Override
    public String describeInstanceConsoleUrl(String instanceId) {
        DescribeInstanceVncUrlRequest request = new DescribeInstanceVncUrlRequest();
        request.setRegionId(config.getRegionId());
        request.setInstanceId(instanceId);
        return tryInvoke(() -> buildClient().describeInstanceVncUrl(request)).getBody().getVncUrl();
    }

    @Override
    public CreateKeyPairResponseBody createKeyPair(CreateKeyPairRequest request) {
        request.setRegionId(config.getRegionId());
        CreateKeyPairResponseBody responseBody = tryInvoke(
                () -> buildClient().createKeyPair(request)
        ).getBody();

        log.info("Aliyun create id pair request sent. RequestId={}. KeyPairName={}.",
                responseBody.getRequestId(), request.getKeyPairName());

        return responseBody;
    }

    @Override
    public void deleteKeyPair(String keyPairName) {
        DeleteKeyPairsRequest request = new DeleteKeyPairsRequest();
        request.setRegionId(config.getRegionId());
        request.setKeyPairNames(keyPairName);

        DeleteKeyPairsResponseBody responseBody = tryInvoke(
                () -> buildClient().deleteKeyPairs(request)
        ).getBody();

        log.info("Aliyun delete id pair request sent. RequestId={}. KeyPairName={}.",
                responseBody.getRequestId(), keyPairName);
    }

    @Override
    public void modifyInstanceChargeType(ModifyInstanceChargeTypeRequest request) {
        request.setRegionId(config.getRegionId());

        if(request.getDryRun() != null && request.getDryRun()){
            tryInvoke(() -> buildClient().modifyInstanceChargeType(request));
        }else {
            ModifyInstanceChargeTypeResponseBody responseBody = tryInvoke(
                    () -> buildClient().modifyInstanceChargeType(request)
            ).getBody();

            log.info("Aliyun modify instance charge type request sent. RequestId={}. InstanceId={}",
                    responseBody.getRequestId(), request.getInstanceIds());

            var feeOfInstances = responseBody.getFeeOfInstances();

            if(feeOfInstances!=null && Utils.isNotEmpty(feeOfInstances.getFeeOfInstance())){
                for (var feeOfInstance : feeOfInstances.getFeeOfInstance()) {
                    log.info("Modify charge type fee of instance {} is {}. RequestId={}.",
                            feeOfInstance.getInstanceId(),
                            feeOfInstance.getFee()+feeOfInstance.getCurrency(), responseBody.getRequestId());
                }
            }
        }
    }

    @Override
    public void modifyInstance(ModifyInstanceAttributeRequest request) {
        var responseBody = tryInvoke(() -> buildClient().modifyInstanceAttribute(request)).getBody();

        log.info("Aliyun modify instance attributes request sent. RequestId={}. InstanceId={}",
                responseBody.getRequestId(), request.getInstanceId());
    }


    @Override
    public DescribeEniMonitorDataResponseBody describeNicMonitorData(DescribeEniMonitorDataRequest request){
        request.setRegionId(config.getRegionId());
        return tryInvoke(() -> buildClient().describeEniMonitorData(request)).getBody();
    }

    @Override
    public DescribeDiskMonitorDataResponseBody describeDiskMonitorData(DescribeDiskMonitorDataRequest request){
        return tryInvoke(() -> buildClient().describeDiskMonitorData(request)).getBody();
    }

    @Override
    public List<AliyunInvocation> describeInvocations(DescribeInvocationsRequest request){
        request.setRegionId(config.getRegionId());
        return queryAllByToken(
                () -> buildClient().describeInvocations(request),
                resp -> resp.getBody().getInvocations().getInvocation(),
                resp -> resp.getBody().getNextToken(),
                request::setNextToken
        ).stream().map(
                i -> new AliyunInvocation(i)
        ).toList();
    }

    @Override
    public Optional<AliyunInvocation> describeInvocation(String invokeId){
        DescribeInvocationsRequest request = new DescribeInvocationsRequest();
        request.setInvokeId(invokeId);
        request.setIncludeOutput(true);
        return describeInvocations(request).stream().findAny();
    }

    private AliyunInvocation getNullableInvocation(String invokeId){
        return describeInvocation(invokeId).orElse(null);
    }

    @Override
    public AliyunInvocation runCommand(RunCommandRequest request) {
        request.setRegionId(config.getRegionId());
        RunCommandResponseBody responseBody = tryInvoke(() -> buildClient().runCommand(request)).getBody();
        String invokeId = responseBody.getInvokeId();

        log.info("Aliyun run command request sent. RequestId={}. InstanceId={}.",
                responseBody.getRequestId(), request.getInstanceId());


        AliyunInvocation invocation;
        int triedTimes = 0;

        while ((invocation = getNullableInvocation(invokeId)) == null || invocation.isRunning()){
            if(triedTimes > 100){
                log.warn("Max tries exceeded for awaiting aliyun command invocation.");
                break;
            }
            triedTimes++;
            SleepUtil.sleep(3);
        }

        if(invocation == null)
            throw new StratoException("Cannot find aliyun invocation %s".formatted(invokeId));

        if(invocation.isRunning())
            log.warn("Waited too long for aliyun invocation {}, status remains {}",
                    invocation, invocation.detail().getInvocationStatus());

        return invocation;
    }

    @Override
    public List<AliyunSnapshot> describeSnapshots(DescribeSnapshotsRequest request) {
        request.setRegionId(config.getRegionId());
        return queryAllByToken(
                () -> buildClient().describeSnapshots(request).getBody(),
                resp -> resp.getSnapshots().getSnapshot(),
                resp -> resp.getNextToken(),
                request::setNextToken
        ).stream().map(
                s -> new AliyunSnapshot(s)
        ).toList();
    }

    @Override
    public Optional<AliyunSnapshot> describeSnapshot(String snapshotId) {
        DescribeSnapshotsRequest request = new DescribeSnapshotsRequest();
        request.setSnapshotIds(JSON.toJsonString(List.of(snapshotId)));
        return describeSnapshots(request).stream().findAny();
    }

    @Override
    public String createSnapshot(CreateSnapshotRequest request) {
        CreateSnapshotResponseBody responseBody = tryInvoke(
                () -> buildClient().createSnapshot(request)
        ).getBody();
        String requestId = responseBody.getRequestId();
        String snapshotId = responseBody.getSnapshotId();

        log.info("Aliyun create snapshot request sent. RequestId={}. SnapshotId={}.",
                requestId, snapshotId);

        return snapshotId;
    }

    @Override
    public void deleteSnapshot(String snapshotId) {
        DeleteSnapshotRequest request = new DeleteSnapshotRequest();
        request.setSnapshotId(snapshotId);
        request.setForce(true);
        DeleteSnapshotResponseBody responseBody = tryInvoke(
                () -> buildClient().deleteSnapshot(request)
        ).getBody();

        log.info("Aliyun delete snapshot request sent. RequestId={}. SnapshotId={}.",
                responseBody.getRequestId(), snapshotId);
    }

    @Override
    public void rollbackToSnapshot(String diskId, String snapshotId, boolean dryRun) {
        ResetDiskRequest request = new ResetDiskRequest();
        request.setSnapshotId(snapshotId);
        request.setDiskId(diskId);
        request.setDryRun(dryRun);

        if(dryRun){
            tryInvoke(() -> buildClient().resetDisk(request));
        } else {
            ResetDiskResponseBody responseBody = tryInvoke(
                    () -> buildClient().resetDisk(request)
            ).getBody();

            log.info("Aliyun reset disk request sent. RequestId={}. SnapshotId={}.",
                    responseBody.getRequestId(), snapshotId);
        }
    }
}
