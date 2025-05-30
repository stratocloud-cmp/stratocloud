package com.stratocloud.provider.tencent.common;

import com.stratocloud.cache.CacheService;
import com.stratocloud.cache.CacheUtil;
import com.stratocloud.exceptions.ExternalAccountInvalidException;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.exceptions.ProviderConnectionException;
import com.stratocloud.exceptions.StratoException;
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
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.TimeUtil;
import com.stratocloud.utils.Utils;
import com.stratocloud.utils.concurrent.SleepUtil;
import com.tencentcloudapi.billing.v20180709.BillingClient;
import com.tencentcloudapi.billing.v20180709.models.DescribeAccountBalanceRequest;
import com.tencentcloudapi.cam.v20190116.CamClient;
import com.tencentcloudapi.cam.v20190116.models.GetUserAppIdRequest;
import com.tencentcloudapi.cam.v20190116.models.GetUserAppIdResponse;
import com.tencentcloudapi.cbs.v20170312.CbsClient;
import com.tencentcloudapi.cbs.v20170312.models.Snapshot;
import com.tencentcloudapi.cbs.v20170312.models.*;
import com.tencentcloudapi.clb.v20180317.ClbClient;
import com.tencentcloudapi.clb.v20180317.models.*;
import com.tencentcloudapi.cloudaudit.v20190319.CloudauditClient;
import com.tencentcloudapi.cloudaudit.v20190319.models.DescribeEventsRequest;
import com.tencentcloudapi.cloudaudit.v20190319.models.DescribeEventsResponse;
import com.tencentcloudapi.cloudaudit.v20190319.models.Event;
import com.tencentcloudapi.cloudaudit.v20190319.models.LookupAttribute;
import com.tencentcloudapi.common.AbstractModel;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.Language;
import com.tencentcloudapi.cvm.v20170312.CvmClient;
import com.tencentcloudapi.cvm.v20170312.models.DescribeRegionsRequest;
import com.tencentcloudapi.cvm.v20170312.models.DescribeRegionsResponse;
import com.tencentcloudapi.cvm.v20170312.models.Filter;
import com.tencentcloudapi.cvm.v20170312.models.Image;
import com.tencentcloudapi.cvm.v20170312.models.Instance;
import com.tencentcloudapi.cvm.v20170312.models.Price;
import com.tencentcloudapi.cvm.v20170312.models.ZoneInfo;
import com.tencentcloudapi.cvm.v20170312.models.*;
import com.tencentcloudapi.monitor.v20180724.MonitorClient;
import com.tencentcloudapi.monitor.v20180724.models.*;
import com.tencentcloudapi.ssl.v20191205.SslClient;
import com.tencentcloudapi.ssl.v20191205.models.*;
import com.tencentcloudapi.tag.v20180813.TagClient;
import com.tencentcloudapi.tag.v20180813.models.AddProjectRequest;
import com.tencentcloudapi.tag.v20180813.models.DescribeProjectsRequest;
import com.tencentcloudapi.tag.v20180813.models.DescribeProjectsResponse;
import com.tencentcloudapi.tag.v20180813.models.Project;
import com.tencentcloudapi.tat.v20201028.TatClient;
import com.tencentcloudapi.tat.v20201028.models.*;
import com.tencentcloudapi.vpc.v20170312.VpcClient;
import com.tencentcloudapi.vpc.v20170312.models.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class TencentCloudClientImpl implements TencentCloudClient{

    private final Credential credential;

    private final String region;

    private final CacheService cacheService;

    public TencentCloudClientImpl(TencentCloudAccountProperties accountProperties,
                                  CacheService cacheService) {
        this.credential = new Credential(accountProperties.getSecretId(), accountProperties.getSecretKey());
        this.region = accountProperties.getRegion();
        this.cacheService = cacheService;
    }

    @Override
    public String getRegion() {
        return region;
    }

    private interface Invoker<R> {
        R invoke() throws TencentCloudSDKException;
    }

    private static class ErrorCodes {
        public static final String SERVICE_UNAVAILABLE = "ServiceUnavailable";
        public static final String RESOURCE_NOT_FOUND = "ResourceNotFound";

        public static final String REQUEST_LIMIT_EXCEEDED = "RequestLimitExceeded";
        public static final String INSTANCE_ID_NOT_FOUND = "InvalidInstanceId.NotFound";

        public static final String RESOURCE_IN_OPERATING = "FailedOperation.ResourceInOperating";
    }

    private static <R> R tryInvoke(Invoker<R> invoker){
        return doTryInvoke(invoker, 0);
    }

    private static <R> R doTryInvoke(Invoker<R> invoker, int triedTimes) {
        if(triedTimes >= 10)
            throw new StratoException("Max triedTimes exceeded: "+triedTimes);

        try {
            return invoker.invoke();
        } catch (TencentCloudSDKException e) {
            if(e.getErrorCode() == null)
                throw new ProviderConnectionException(e.getMessage(), e);

            if(e.getErrorCode().startsWith("AuthFailure"))
                throw new ExternalAccountInvalidException(e.getMessage(), e);

            log.warn("ErrorCode: {}", e.getErrorCode());

            switch (e.getErrorCode()) {
                case ErrorCodes.SERVICE_UNAVAILABLE -> throw new ProviderConnectionException(e.getMessage(), e);
                case ErrorCodes.RESOURCE_NOT_FOUND, ErrorCodes.INSTANCE_ID_NOT_FOUND ->
                        throw new ExternalResourceNotFoundException(e.getMessage(), e);
                case ErrorCodes.REQUEST_LIMIT_EXCEEDED, ErrorCodes.RESOURCE_IN_OPERATING -> {
                    log.warn("Retrying later: {}", e.getMessage());
                    SleepUtil.sleepRandomlyByMilliSeconds(500, 3000);
                    return doTryInvoke(invoker, triedTimes+1);
                }
                default -> throw new StratoException(e.getMessage(), e);
            }
        }catch (Exception e){
            throw new ProviderConnectionException(e.getMessage(), e);
        }
    }

    private static ClientProfile createClientProfile() {
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setLanguage(Language.ZH_CN);
        return clientProfile;
    }

    private CvmClient buildCvmClient() {
        return new CvmClient(credential, region, createClientProfile());
    }

    private VpcClient buildVpcClient() {
        return new VpcClient(credential, region, createClientProfile());
    }

    private CbsClient buildCbsClient() {
        return new CbsClient(credential, region, createClientProfile());
    }

    private BillingClient buildBillingClient() {
        return new BillingClient(credential, region, createClientProfile());
    }

    private TagClient buildTagClient(){
        return new TagClient(credential, region, createClientProfile());
    }

    private ClbClient buildClbClient(){
        return new ClbClient(credential, region, createClientProfile());
    }

    private SslClient buildSslClient(){
        return new SslClient(credential, region, createClientProfile());
    }

    private MonitorClient buildMonitorClient(){
        return new MonitorClient(credential, region, createClientProfile());
    }

    private CamClient buildCamClient(){
        return new CamClient(credential, region, createClientProfile());
    }

    private TatClient buildTatClient(){
        return new TatClient(credential, region, createClientProfile());
    }

    private CloudauditClient buildAuditClient(){
        return new CloudauditClient(credential, region, createClientProfile());
    }

    private String buildCacheKey(String targetName, AbstractModel queryRequest){
        return "TencentCloud-%s-ofRegion-%s-andSecretId-%s-andRequest-%s".formatted(
                targetName, region, credential.getSecretId(), JSON.toJsonString(queryRequest)
        );
    }

    private String buildCacheKey(String targetName){
        return "TencentCloud-%s-ofRegion-%s-andSecretId-%s".formatted(
                targetName, region, credential.getSecretId()
        );
    }

    private <E, R> List<E> queryAll(Invoker<R> invoker,
                                    Function<R, E[]> listGetter,
                                    Function<R, Long> totalCountGetter,
                                    Consumer<Long> offsetSetter,
                                    Consumer<Long> limitSetter){
        try {
            List<E> result = new ArrayList<>();

            final long limit = 100L;
            limitSetter.accept(limit);

            long offset = 0L;

            long totalCount = limit;

            while (offset < totalCount){
                offsetSetter.accept(offset);
                R response = tryInvoke(invoker);
                totalCount = totalCountGetter.apply(response);
                E[] page = listGetter.apply(response);

                if(Utils.isNotEmpty(page))
                    result.addAll(Arrays.asList(page));

                offset = offset + limit;
            }

            return result;
        }catch (ExternalResourceNotFoundException e){
            return List.of();
        }
    }

    private  <E, R> List<E> queryAllByPage(Invoker<R> invoker,
                                           Function<R, E[]> listGetter,
                                           Function<R, Long> totalCountGetter,
                                           Consumer<Long> pageNumberSetter,
                                           Consumer<Long> pageSizeSetter){
        try {
            List<E> result = new ArrayList<>();

            final long pageSize = 50;
            pageSizeSetter.accept(pageSize);

            long pageNumber = 1;

            long totalCount = 1;

            while (pageNumber <= (totalCount/pageSize + (totalCount%pageSize==0?0:1))){
                pageNumberSetter.accept(pageNumber);
                R response = tryInvoke(invoker);
                totalCount = totalCountGetter.apply(response);
                E[] page = listGetter.apply(response);

                if(Utils.isNotEmpty(page)) {
                    List<E> list = List.of(page);
                    result.addAll(list);
                }

                pageNumber++;
            }

            return result;
        }catch (ExternalResourceNotFoundException e){
            return List.of();
        }
    }

    @Override
    public void validateConnection() {
        CvmClient cvmClient = buildCvmClient();

        DescribeRegionsResponse response = tryInvoke(()->cvmClient.DescribeRegions(new DescribeRegionsRequest()));

        if(Utils.isEmpty(response.getRegionSet()))
            throw new ProviderConnectionException(
                    "Cannot get regions from tencent cloud. Using region: "+cvmClient.getRegion()
            );
    }

    private Project ensureDefaultProject(){
        String defaultProjectName = "StratoDefaultProject";

        DescribeProjectsRequest describeProjectsRequest = new DescribeProjectsRequest();
        describeProjectsRequest.setProjectName(defaultProjectName);

        List<Project> projects = describeProjects(describeProjectsRequest);

        if(Utils.isNotEmpty(projects))
            return projects.get(0);

        AddProjectRequest addProjectRequest = new AddProjectRequest();
        addProjectRequest.setProjectName(defaultProjectName);

        Long projectId = tryInvoke(() -> buildTagClient().AddProject(addProjectRequest)).getProjectId();
        describeProjectsRequest = new DescribeProjectsRequest();
        describeProjectsRequest.setProjectId(projectId);
        return describeProjects(describeProjectsRequest).stream().findAny().orElseThrow();
    }

    private List<Project> describeProjects(DescribeProjectsRequest request){
        request.setAllList(1L);
        return queryAll(
                () -> buildTagClient().DescribeProjects(request),
                DescribeProjectsResponse::getProjects,
                DescribeProjectsResponse::getTotal,
                request::setOffset,
                request::setLimit
        );
    }


    @Override
    public Float describeBalance() {
        return tryInvoke(
                () -> buildBillingClient().DescribeAccountBalance(
                        new DescribeAccountBalanceRequest()
                )
        ).getRealBalance()/100;
    }

    @Override
    public List<ZoneInfo> describeZones(){
        Supplier<List<ZoneInfo>> queryFunction = this::doDescribeZones;

        return CacheUtil.queryWithCache(
                cacheService,
                buildCacheKey("Zones"),
                300L,
                queryFunction,
                new ArrayList<>()
        );
    }

    private List<ZoneInfo> doDescribeZones() {
        List<ZoneInfo> result = new ArrayList<>();

        CvmClient cvmClient = buildCvmClient();

        ZoneInfo[] zoneSet = tryInvoke(
                () -> cvmClient.DescribeZones(new DescribeZonesRequest())
        ).getZoneSet();

        if(zoneSet != null)
            result.addAll(Arrays.asList(zoneSet));

        return result;
    }

    @Override
    public Optional<ZoneInfo> describeZone(String zone){
        return describeZones().stream().filter(z -> Objects.equals(zone, z.getZone())).findAny();
    }


    @Override
    public List<Vpc> describeVpcs(DescribeVpcsRequest request){
        VpcClient vpcClient = buildVpcClient();
        return queryAll(
                () -> vpcClient.DescribeVpcs(request),
                DescribeVpcsResponse::getVpcSet,
                DescribeVpcsResponse::getTotalCount,
                offset -> request.setOffset(String.valueOf(offset)),
                limit -> request.setLimit(String.valueOf(limit))
        );
    }

    @Override
    public Optional<Vpc> describeVpc(String vpcId){
        DescribeVpcsRequest request = new DescribeVpcsRequest();
        request.setVpcIds(new String[]{vpcId});
        return describeVpcs(request).stream().findAny();
    }

    @Override
    public List<InstanceTypeConfig> describeInstanceTypes(DescribeInstanceTypeConfigsRequest request){
        Supplier<List<InstanceTypeConfig>> queryFunction = () -> doDescribeInstanceTypes(request);

        return CacheUtil.queryWithCache(
                cacheService,
                buildCacheKey("InstanceTypes", request),
                300L,
                queryFunction,
                new ArrayList<>()
        );
    }

    private List<InstanceTypeConfig> doDescribeInstanceTypes(DescribeInstanceTypeConfigsRequest request) {
        List<InstanceTypeConfig> result = new ArrayList<>();

        CvmClient cvmClient = buildCvmClient();

        var set = tryInvoke(() -> cvmClient.DescribeInstanceTypeConfigs(request)).getInstanceTypeConfigSet();

        if(set != null)
            result.addAll(Arrays.asList(set));

        return result;
    }

    @Override
    public Optional<InstanceTypeConfig> describeInstanceType(TencentFlavorId flavorId){
        DescribeInstanceTypeConfigsRequest request = new DescribeInstanceTypeConfigsRequest();

        Filter[] filters = FilterFactory.createFlavorFilter(flavorId);

        request.setFilters(filters);

        return describeInstanceTypes(request).stream().findAny();
    }


    @Override
    public Optional<InstanceTypeQuotaItem> describeInstanceTypeQuotaItem(TencentFlavorId flavorId) {
        DescribeZoneInstanceConfigInfosRequest request = new DescribeZoneInstanceConfigInfosRequest();

        request.setFilters(FilterFactory.createFlavorFilter(flavorId));

        List<InstanceTypeQuotaItem> quotaItems = describeInstanceTypeQuotaItems(request);

        return quotaItems.stream().findAny();
    }

    @Override
    public List<InstanceTypeQuotaItem> describeInstanceTypeQuotaItems(DescribeZoneInstanceConfigInfosRequest request) {
        return CacheUtil.queryWithCache(
                cacheService,
                buildCacheKey("InstanceTypeQuotaItems", request),
                300L,
                () -> doDescribeInstanceTypeQuotaItems(request),
                new ArrayList<>()
        );
    }

    private List<InstanceTypeQuotaItem> doDescribeInstanceTypeQuotaItems(DescribeZoneInstanceConfigInfosRequest request) {
        InstanceTypeQuotaItem[] quotaItems = tryInvoke(
                () -> buildCvmClient().DescribeZoneInstanceConfigInfos(request)
        ).getInstanceTypeQuotaSet();

        if(quotaItems == null)
            return new ArrayList<>();

        return List.of(quotaItems);
    }

    @Override
    public List<InstanceFamilyConfig> describeInstanceFamilies() {
        Supplier<List<InstanceFamilyConfig>> queryFunction = this::doDescribeInstanceFamilies;
        return CacheUtil.queryWithCache(
                cacheService,
                buildCacheKey("InstanceFamilies"),
                300L,
                queryFunction,
                new ArrayList<>()
        );
    }

    private List<InstanceFamilyConfig> doDescribeInstanceFamilies() {
        List<InstanceFamilyConfig> result = new ArrayList<>();

        CvmClient cvmClient = buildCvmClient();

        InstanceFamilyConfig[] familyConfigSet = tryInvoke(
                () -> cvmClient.DescribeInstanceFamilyConfigs(new DescribeInstanceFamilyConfigsRequest())
        ).getInstanceFamilyConfigSet();

        if(familyConfigSet != null)
            result.addAll(Arrays.asList(familyConfigSet));

        return result;
    }


    @Override
    public List<Image> describeImages(DescribeImagesRequest request) {
        if(Utils.isEmpty(request.getImageIds())){
            Filter filter = new Filter();
            filter.setName("image-type");
            filter.setValues(new String[]{"PRIVATE_IMAGE", "PUBLIC_IMAGE", "SHARED_IMAGE"});
        }

        return CacheUtil.queryWithCache(
                cacheService,
                buildCacheKey("Images", request),
                300,
                () -> doDescribeImages(request),
                new ArrayList<>()
        );
    }

    private List<Image> doDescribeImages(DescribeImagesRequest request) {
        return queryAll(
                () -> buildCvmClient().DescribeImages(request),
                DescribeImagesResponse::getImageSet,
                DescribeImagesResponse::getTotalCount,
                request::setOffset,
                request::setLimit
        );
    }


    @Override
    public Optional<Image> describeImage(String imageId) {
        DescribeImagesRequest request = new DescribeImagesRequest();

        request.setImageIds(new String[]{imageId});

        return describeImages(request).stream().findAny();
    }

    @Override
    public Optional<Subnet> describeSubnet(String subnetId) {
        DescribeSubnetsRequest request = new DescribeSubnetsRequest();
        request.setSubnetIds(new String[]{subnetId});
        return describeSubnets(request).stream().findAny();
    }

    @Override
    public List<Subnet> describeSubnets(DescribeSubnetsRequest request) {
        return queryAll(
                () -> buildVpcClient().DescribeSubnets(request),
                DescribeSubnetsResponse::getSubnetSet,
                DescribeSubnetsResponse::getTotalCount,
                offset -> request.setOffset(String.valueOf(offset)),
                limit -> request.setLimit(String.valueOf(limit))
        );
    }


    @Override
    public List<SecurityGroup> describeSecurityGroups(DescribeSecurityGroupsRequest request) {
        return queryAll(
                () -> buildVpcClient().DescribeSecurityGroups(request),
                DescribeSecurityGroupsResponse::getSecurityGroupSet,
                DescribeSecurityGroupsResponse::getTotalCount,
                offset -> request.setOffset(offset.toString()),
                limit -> request.setLimit(limit.toString())
        );
    }

    @Override
    public Optional<SecurityGroup> describeSecurityGroup(String securityGroupId) {
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();
        request.setSecurityGroupIds(new String[]{securityGroupId});
        return describeSecurityGroups(request).stream().findAny();
    }

    @Override
    public List<SecurityGroupPolicy> describeSecurityGroupPolicies(SecurityGroupPolicyDirection direction) {
        List<SecurityGroupPolicy> result = new ArrayList<>();

        List<SecurityGroup> securityGroups = describeSecurityGroups(new DescribeSecurityGroupsRequest());

        for (SecurityGroup securityGroup : securityGroups) {
            String securityGroupId = securityGroup.getSecurityGroupId();

            SecurityGroupPolicy[] policies = describeSecurityGroupPolicies(securityGroupId, direction);

            if(policies != null)
                result.addAll(Arrays.asList(policies));
        }

        return result;
    }


    @Override
    public SecurityGroupPolicy[] describeSecurityGroupPolicies(String securityGroupId,
                                                               SecurityGroupPolicyDirection direction) {
        try {
            DescribeSecurityGroupPoliciesRequest request = new DescribeSecurityGroupPoliciesRequest();
            request.setSecurityGroupId(securityGroupId);

            SecurityGroupPolicySet policySet = tryInvoke(
                    () -> buildVpcClient().DescribeSecurityGroupPolicies(request)
            ).getSecurityGroupPolicySet();

            SecurityGroupPolicy[] result =  switch (direction){
                case ingress -> policySet.getIngress();
                case egress -> policySet.getEgress();
            };

            if(Utils.isNotEmpty(result))
                for (SecurityGroupPolicy policy : result) {
                    policy.setSecurityGroupId(securityGroupId);
                }

            return result;
        }catch (ExternalResourceNotFoundException e){
            return new SecurityGroupPolicy[0];
        }
    }

    @Override
    public Optional<SecurityGroupPolicy> describeSecurityGroupPolicy(TencentSecurityGroupPolicyId policyId) {
        String securityGroupId = policyId.securityGroupId();
        SecurityGroupPolicyDirection direction = policyId.policyDirection();

        SecurityGroupPolicy[] policies = describeSecurityGroupPolicies(securityGroupId, direction);

        if(policies == null)
            return Optional.empty();

        return Arrays.stream(policies).filter(policyId::isSamePolicy).findAny();
    }


    @Override
    public void createSecurityGroupPolicies(CreateSecurityGroupPoliciesRequest request) {
        var response = tryInvoke(() -> buildVpcClient().CreateSecurityGroupPolicies(request));
        log.info("Tencent create security group policies request sent. RequestId={}.", response.getRequestId());
    }

    @Override
    public void removeSecurityGroupPolicy(TencentSecurityGroupPolicyId policyId) {
        SecurityGroupPolicySet policySet = new SecurityGroupPolicySet();

        Optional<SecurityGroupPolicy> policyOptional = describeSecurityGroupPolicy(policyId);

        if(policyOptional.isEmpty())
            return;

        SecurityGroupPolicy policy = policyOptional.map(p1 -> {
            SecurityGroupPolicy p2 = new SecurityGroupPolicy();
            p2.setPolicyIndex(p1.getPolicyIndex());
            return p2;
        }).get();

        switch (policyId.policyDirection()){
            case egress -> policySet.setEgress(new SecurityGroupPolicy[]{policy});
            case ingress -> policySet.setIngress(new SecurityGroupPolicy[]{policy});
        }

        DeleteSecurityGroupPoliciesRequest request = new DeleteSecurityGroupPoliciesRequest();
        request.setSecurityGroupId(policyId.securityGroupId());
        request.setSecurityGroupPolicySet(policySet);

        try {
            var response = tryInvoke(() -> buildVpcClient().DeleteSecurityGroupPolicies(request));
            log.info("Tencent remove security group policy request sent. PolicyId={}. RequestId={}.",
                    policyId, response.getRequestId());
        }catch (ExternalResourceNotFoundException e){
            log.warn(e.getMessage());
        }
    }

    @Override
    public SecurityGroup createSecurityGroup(CreateSecurityGroupRequest request) {
        var response = tryInvoke(() -> buildVpcClient().CreateSecurityGroup(request));
        SecurityGroup securityGroup = response.getSecurityGroup();

        log.info("Tencent create security group request sent. SecurityGroupId={}. RequestId={}.",
                securityGroup.getSecurityGroupId(), response.getRequestId());

        return securityGroup;
    }

    @Override
    public void deleteSecurityGroup(String securityGroupId) {
        DeleteSecurityGroupRequest request = new DeleteSecurityGroupRequest();
        request.setSecurityGroupId(securityGroupId);
        try {
            var response = tryInvoke(() -> buildVpcClient().DeleteSecurityGroup(request));

            log.info("Tencent delete security group request sent. SecurityGroupId={}. RequestId={}.",
                    securityGroupId, response.getRequestId());
        }catch (ExternalResourceNotFoundException e){
            log.warn(e.getMessage());
        }
    }

    @Override
    public Vpc createVpc(CreateVpcRequest request) {
        CreateVpcResponse response = tryInvoke(() -> buildVpcClient().CreateVpc(request));

        log.info("Tencent create vpc request sent. VpcId={}. RequestId={}.",
                response.getVpc().getVpcId(), response.getRequestId());

        return response.getVpc();
    }

    @Override
    public void deleteVpc(String vpcId) {
        DeleteVpcRequest request = new DeleteVpcRequest();
        request.setVpcId(vpcId);
        DeleteVpcResponse response = tryInvoke(() -> buildVpcClient().DeleteVpc(request));
        log.info("Tencent delete vpc request sent. VpcId={}. RequestId={}.",
                vpcId, response.getRequestId());
    }

    @Override
    public Subnet createSubnet(CreateSubnetRequest request) {
        CreateSubnetResponse response = tryInvoke(() -> buildVpcClient().CreateSubnet(request));
        log.info("Tencent create subnet request sent. SubnetId={}. RequestId={}.",
                response.getSubnet().getSubnetId(), response.getRequestId());
        return response.getSubnet();
    }

    @Override
    public void deleteSubnet(String subnetId) {
        DeleteSubnetRequest request = new DeleteSubnetRequest();
        request.setSubnetId(subnetId);

        DeleteSubnetResponse response = tryInvoke(() -> buildVpcClient().DeleteSubnet(request));
        log.info("Tencent delete subnet request sent. SubnetId={}. RequestId={}.",
                subnetId, response.getRequestId());
    }

    @Override
    public List<NetworkInterface> describeNics(DescribeNetworkInterfacesRequest request) {
        return queryAll(
                () -> buildVpcClient().DescribeNetworkInterfaces(request),
                DescribeNetworkInterfacesResponse::getNetworkInterfaceSet,
                DescribeNetworkInterfacesResponse::getTotalCount,
                request::setOffset,
                request::setLimit
        );
    }

    @Override
    public Optional<NetworkInterface> describeNic(String nicId) {
        DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();
        request.setNetworkInterfaceIds(new String[]{nicId});

        return describeNics(request).stream().findAny();
    }

    @Override
    public Optional<NetworkInterface> describePrimaryNicByInstanceId(String instanceId) {
        var filter = new com.tencentcloudapi.vpc.v20170312.models.Filter();
        filter.setName("attachment.instance-id");
        filter.setValues(new String[]{instanceId});

        DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();
        request.setFilters(new com.tencentcloudapi.vpc.v20170312.models.Filter[]{filter});

        List<NetworkInterface> networkInterfaces = describeNics(request);

        return networkInterfaces.stream().filter(nic -> nic.getPrimary() != null && nic.getPrimary()).findAny();
    }

    @Override
    public NetworkInterface createNic(CreateNetworkInterfaceRequest request) {
        var response = tryInvoke(() -> buildVpcClient().CreateNetworkInterface(request));
        log.info("Tencent create nic request sent. NicId={}. RequestId={}.",
                response.getNetworkInterface().getNetworkInterfaceId(), response.getRequestId());
        return response.getNetworkInterface();
    }

    @Override
    public void deleteNic(String nicId) {
        DeleteNetworkInterfaceRequest request = new DeleteNetworkInterfaceRequest();
        request.setNetworkInterfaceId(nicId);

        var response = tryInvoke(() -> buildVpcClient().DeleteNetworkInterface(request));

        log.info("Tencent delete nic request sent. NicId={}. RequestId={}.",
                nicId, response.getRequestId());
    }


    @Override
    public Optional<Instance> describeInstance(String instanceId) {
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        request.setInstanceIds(new String[]{instanceId});
        return describeInstances(request).stream().findAny();
    }

    @Override
    public List<Instance> describeInstances(DescribeInstancesRequest request){
        return queryAll(
                () -> buildCvmClient().DescribeInstances(request),
                DescribeInstancesResponse::getInstanceSet,
                DescribeInstancesResponse::getTotalCount,
                request::setOffset,
                request::setLimit
        );
    }

    @Override
    public String runInstance(RunInstancesRequest request) {
        if(request.getInstanceCount() != null && request.getInstanceCount() > 1)
            throw new StratoException("Do not run multiple instances through this api.");

        RunInstancesResponse response = tryInvoke(() -> buildCvmClient().RunInstances(request));
        String[] instanceIdSet = response.getInstanceIdSet();

        if(Utils.isEmpty(instanceIdSet))
            return null;

        log.info("Tencent create instance request sent. InstanceId={}. RequestId={}.",
                instanceIdSet[0], response.getRequestId());

        return instanceIdSet[0];
    }

    @Override
    public Optional<Disk> describeSystemDiskByInstanceId(String instanceId) {
        DescribeDisksRequest request = new DescribeDisksRequest();
        var filter = new com.tencentcloudapi.cbs.v20170312.models.Filter();

        filter.setName("instance-id");
        filter.setValues(new String[]{instanceId});

        request.setFilters(new com.tencentcloudapi.cbs.v20170312.models.Filter[]{filter});

        List<Disk> disks = describeDisks(request);

        return disks.stream().filter(disk -> "SYSTEM_DISK".equalsIgnoreCase(disk.getDiskUsage())).findAny();
    }

    @Override
    public List<Disk> describeDisks(DescribeDisksRequest request) {
        return queryAll(
                () -> buildCbsClient().DescribeDisks(request),
                DescribeDisksResponse::getDiskSet,
                DescribeDisksResponse::getTotalCount,
                request::setOffset,
                request::setLimit
        );
    }

    @Override
    public Optional<Disk> describeDisk(String diskId) {
        DescribeDisksRequest request = new DescribeDisksRequest();
        request.setDiskIds(new String[]{diskId});
        return describeDisks(request).stream().findAny();
    }

    @Override
    public void terminateInstance(String instanceId) {
        TerminateInstancesRequest request = new TerminateInstancesRequest();

        request.setInstanceIds(new String[]{instanceId});
        request.setReleasePrepaidDataDisks(false);

        TerminateInstancesResponse response = tryInvoke(() -> buildCvmClient().TerminateInstances(request));
        log.info("Tencent terminate instance request sent. InstanceId={}. RequestId={}.",
                instanceId, response.getRequestId());
    }

    @Override
    public void attachNic(String instanceId, String nicId) {
        AttachNetworkInterfaceRequest request = new AttachNetworkInterfaceRequest();

        request.setInstanceId(instanceId);
        request.setNetworkInterfaceId(nicId);

        var response = tryInvoke(() -> buildVpcClient().AttachNetworkInterface(request));

        log.info("Tencent attach nic request sent. NicId={}. RequestId={}.",
                nicId, response.getRequestId());

        waitForVpcTask(response.getRequestId());
    }

    @Override
    public void detachNic(String instanceId, String nicId) {
        DetachNetworkInterfaceRequest request = new DetachNetworkInterfaceRequest();

        request.setInstanceId(instanceId);
        request.setNetworkInterfaceId(nicId);

        var response = tryInvoke(() -> buildVpcClient().DetachNetworkInterface(request));

        log.info("Tencent detach nic request sent. NicId={}. RequestId={}.",
                nicId, response.getRequestId());

        waitForVpcTask(response.getRequestId());
    }

    private void waitForVpcTask(String requestId){
        DescribeVpcTaskResultRequest request = new DescribeVpcTaskResultRequest();
        request.setTaskId(requestId);

        VpcClient client = buildVpcClient();

        int count = 0;

        while (count < 30) {
            var response = tryInvoke(() -> client.DescribeVpcTaskResult(request));
            String status = response.getStatus();

            count++;

            switch (status){
                case "SUCCESS" -> {
                    log.info("Tencent vpc task {} succeeded.", requestId);
                    return;
                }
                case "FAILED" -> {
                    log.error("Tencent vpc task {} failed. Output={}.", requestId, response.getOutput());
                    return;
                }
                case "RUNNING" -> log.warn("Tencent vpc task {} is still running.", requestId);
                default -> log.error("Unknown vpc task status {}.", status);
            }

            SleepUtil.sleep(10);
        }

        log.error("Waited too long for tencent vpc task {}, there might be a problem.", requestId);
    }



    @Override
    public void modifyNic(ModifyNetworkInterfaceAttributeRequest modifyRequest) {
        var response = tryInvoke(() -> buildVpcClient().ModifyNetworkInterfaceAttribute(modifyRequest));

        log.info("Tencent modify nic request sent. NicId={}. RequestId={}.",
                modifyRequest.getNetworkInterfaceId(), response.getRequestId());
    }

    @Override
    public void modifyNicQosLevel(ModifyNetworkInterfaceQosRequest modifyQosRequest) {
        var response = tryInvoke(() -> buildVpcClient().ModifyNetworkInterfaceQos(modifyQosRequest));

        log.info("Tencent modify nic qos level request sent. NicId={}. RequestId={}.",
                modifyQosRequest.getNetworkInterfaceIds(), response.getRequestId());
    }

    @Override
    public void assignPrivateIps(AssignPrivateIpAddressesRequest assignRequest) {
        var response = tryInvoke(() -> buildVpcClient().AssignPrivateIpAddresses(assignRequest));

        log.info("Tencent assign private ip request sent. NicId={}. Ips={}. RequestId={}.",
                assignRequest.getNetworkInterfaceId(), assignRequest.getPrivateIpAddresses(), response.getRequestId());
    }

    @Override
    public String createDisk(CreateDisksRequest request) {
        if(request.getDiskCount() != null && request.getDiskCount() > 1)
            throw new StratoException("Do not create multiple disks through this api.");

        CreateDisksResponse response = tryInvoke(() -> buildCbsClient().CreateDisks(request));

        log.info("Tencent create disk request sent. DiskId={}. RequestId={}.",
                response.getDiskIdSet()[0], response.getRequestId());

        return response.getDiskIdSet()[0];
    }

    @Override
    public void modifyDisk(ModifyDiskAttributesRequest modifyRequest) {
        var response = tryInvoke(() -> buildCbsClient().ModifyDiskAttributes(modifyRequest));

        log.info("Tencent modify disk request sent. DiskId={}. RequestId={}.",
                modifyRequest.getDiskIds()[0], response.getRequestId());
    }

    @Override
    public void modifyDiskBackupQuota(ModifyDiskBackupQuotaRequest modifyBackupQuotaRequest) {
        var response = tryInvoke(() -> buildCbsClient().ModifyDiskBackupQuota(modifyBackupQuotaRequest));

        log.info("Tencent modify disk backup quota request sent. DiskId={}. RequestId={}.",
                modifyBackupQuotaRequest.getDiskId(), response.getRequestId());
    }


    @Override
    public void modifyDiskExtraPerformance(ModifyDiskExtraPerformanceRequest modifyDiskExtraPerformanceRequest) {
        var response = tryInvoke(() -> buildCbsClient().ModifyDiskExtraPerformance(modifyDiskExtraPerformanceRequest));

        log.info("Tencent modify disk extra performance request sent. DiskId={}. RequestId={}.",
                modifyDiskExtraPerformanceRequest.getDiskId(), response.getRequestId());
    }

    @Override
    public void deleteDisk(String diskId) {
        TerminateDisksRequest request = new TerminateDisksRequest();
        request.setDiskIds(new String[]{diskId});
        TerminateDisksResponse response = tryInvoke(() -> buildCbsClient().TerminateDisks(request));

        log.info("Tencent delete disk request sent. DiskId={}. RequestId={}.",
                diskId, response.getRequestId());
    }

    @Override
    public void attachDisk(String instanceId, String diskId) {
        AttachDisksRequest request = new AttachDisksRequest();
        request.setInstanceId(instanceId);
        request.setDiskIds(new String[]{diskId});
        AttachDisksResponse response = tryInvoke(() -> buildCbsClient().AttachDisks(request));

        log.info("Tencent attach disk request sent. DiskId={}. RequestId={}.",
                diskId, response.getRequestId());
    }

    @Override
    public void detachDisk(String instanceId, String diskId) {
        DetachDisksRequest request = new DetachDisksRequest();
        request.setInstanceId(instanceId);
        request.setDiskIds(new String[]{diskId});
        DetachDisksResponse response = tryInvoke(() -> buildCbsClient().DetachDisks(request));

        log.info("Tencent detach disk request sent. DiskId={}. RequestId={}.",
                diskId, response.getRequestId());
    }

    @Override
    public void resizeDisk(ResizeDiskRequest request) {
        ResizeDiskResponse response = tryInvoke(() -> buildCbsClient().ResizeDisk(request));

        log.info("Tencent resize disk request sent. DiskId={}. RequestId={}.",
                request.getDiskId(), response.getRequestId());
    }

    @Override
    public void resizeInstanceDisks(ResizeInstanceDisksRequest request) {
        ResizeInstanceDisksResponse response = tryInvoke(() -> buildCvmClient().ResizeInstanceDisks(request));

        log.info("Tencent resize instance disk request sent. InstanceId={}. RequestId={}.",
                request.getInstanceId(), response.getRequestId());
    }

    @Override
    public void resetInstanceType(ResetInstancesTypeRequest request) {
        ResetInstancesTypeResponse response = tryInvoke(() -> buildCvmClient().ResetInstancesType(request));

        log.info("Tencent reset instances type request sent. InstanceIds={}. RequestId={}.",
                List.of(request.getInstanceIds()), response.getRequestId());
    }

    @Override
    public void resetInstance(ResetInstanceRequest request) {
        ResetInstanceResponse response = tryInvoke(() -> buildCvmClient().ResetInstance(request));

        log.info("Tencent reset instance request sent. InstanceId={}. RequestId={}.",
                request.getInstanceId(), response.getRequestId());
    }

    @Override
    public void startInstance(String instanceId) {
        StartInstancesRequest request = new StartInstancesRequest();
        request.setInstanceIds(new String[]{instanceId});
        StartInstancesResponse response = tryInvoke(() -> buildCvmClient().StartInstances(request));

        log.info("Tencent start instance request sent. InstanceId={}. RequestId={}.",
                instanceId, response.getRequestId());
    }

    @Override
    public void stopInstance(String instanceId, String stopType) {
        StopInstancesRequest request = new StopInstancesRequest();
        request.setInstanceIds(new String[]{instanceId});

        if(Utils.isNotBlank(stopType))
            request.setStopType(stopType);

        StopInstancesResponse response = tryInvoke(() -> buildCvmClient().StopInstances(request));

        log.info("Tencent stop instance request sent. InstanceId={}. RequestId={}.",
                instanceId, response.getRequestId());
    }

    @Override
    public void restartInstance(String instanceId, String stopType) {
        RebootInstancesRequest request = new RebootInstancesRequest();
        request.setInstanceIds(new String[]{instanceId});

        if(Utils.isNotBlank(stopType))
            request.setStopType(stopType);

        RebootInstancesResponse response = tryInvoke(() -> buildCvmClient().RebootInstances(request));

        log.info("Tencent restart instance request sent. InstanceId={}. RequestId={}.",
                instanceId, response.getRequestId());
    }


    @Override
    public List<KeyPair> describeKeyPairs(DescribeKeyPairsRequest request) {
        return queryAll(
                () -> buildCvmClient().DescribeKeyPairs(request),
                DescribeKeyPairsResponse::getKeyPairSet,
                DescribeKeyPairsResponse::getTotalCount,
                request::setOffset,
                request::setLimit
        );
    }

    @Override
    public Optional<KeyPair> describeKeyPair(String keyPairId) {
        DescribeKeyPairsRequest request = new DescribeKeyPairsRequest();
        request.setKeyIds(new String[]{keyPairId});
        return describeKeyPairs(request).stream().findAny();
    }



    @Override
    public KeyPair createKeyPair(CreateKeyPairRequest request) {
        if(request.getProjectId() == null){
            Project project = ensureDefaultProject();
            request.setProjectId(project.getProjectId());
        }

        CreateKeyPairResponse response = tryInvoke(() -> buildCvmClient().CreateKeyPair(request));

        log.info("Tencent create keypair request sent. KeyId={}. RequestId={}.",
                response.getKeyPair().getKeyId(), response.getRequestId());

        return response.getKeyPair();
    }

    @Override
    public void deleteKeyPair(String keyPairId) {
        DeleteKeyPairsRequest request = new DeleteKeyPairsRequest();
        request.setKeyIds(new String[]{keyPairId});
        DeleteKeyPairsResponse response = tryInvoke(() -> buildCvmClient().DeleteKeyPairs(request));

        log.info("Tencent delete keypair request sent. KeyId={}. RequestId={}.",
                keyPairId, response.getRequestId());
    }

    @Override
    public void associateKeyPairs(String instanceId,
                                  List<String> keyPairIds) {
        Instance instance = describeInstance(instanceId).orElseThrow(
                () -> new ExternalResourceNotFoundException("Instance not found: " + instanceId)
        );

        if(instance.getLoginSettings() != null && Utils.isNotEmpty(instance.getLoginSettings().getKeyIds()))
            if(Set.of(instance.getLoginSettings().getKeyIds()).containsAll(keyPairIds))
                return;

        AssociateInstancesKeyPairsRequest request = new AssociateInstancesKeyPairsRequest();
        request.setInstanceIds(new String[]{instanceId});
        request.setKeyIds(keyPairIds.toArray(String[]::new));
        var response = tryInvoke(() -> buildCvmClient().AssociateInstancesKeyPairs(request));

        log.info("Tencent associate id pairs request sent. KeyIds={}. RequestId={}.",
                keyPairIds, response.getRequestId());
    }

    @Override
    public void disassociateKeyPair(String instanceId, String keyPairId) {
        DisassociateInstancesKeyPairsRequest request = new DisassociateInstancesKeyPairsRequest();
        request.setInstanceIds(new String[]{instanceId});
        request.setKeyIds(new String[]{keyPairId});

        var response = tryInvoke(() -> buildCvmClient().DisassociateInstancesKeyPairs(request));

        log.info("Tencent disassociate id pairs request sent. KeyId={}. RequestId={}.",
                keyPairId, response.getRequestId());
    }


    @Override
    public Optional<Address> describeEip(String eipId) {
        DescribeAddressesRequest request = new DescribeAddressesRequest();
        request.setAddressIds(new String[]{eipId});
        return describeEips(request).stream().findAny();
    }

    @Override
    public List<Address> describeEips(DescribeAddressesRequest request) {
        return queryAll(
                () -> buildVpcClient().DescribeAddresses(request),
                DescribeAddressesResponse::getAddressSet,
                DescribeAddressesResponse::getTotalCount,
                request::setOffset,
                request::setLimit
        );
    }


    @Override
    public Optional<BandwidthPackage> describeBandwidthPackage(String packageId) {
        DescribeBandwidthPackagesRequest request = new DescribeBandwidthPackagesRequest();
        request.setBandwidthPackageIds(new String[]{packageId});
        return describeBandwidthPackages(request).stream().findAny();
    }

    @Override
    public List<BandwidthPackage> describeBandwidthPackages(DescribeBandwidthPackagesRequest request) {
        return queryAll(
                () -> buildVpcClient().DescribeBandwidthPackages(request),
                DescribeBandwidthPackagesResponse::getBandwidthPackageSet,
                DescribeBandwidthPackagesResponse::getTotalCount,
                request::setOffset,
                request::setLimit
        );
    }

    @Override
    public String createEip(AllocateAddressesRequest request) {
        if(request.getAddressCount() != null && request.getAddressCount() > 1)
            throw new StratoException("Do not create multiple eips through this api.");

        AllocateAddressesResponse response = tryInvoke(() -> buildVpcClient().AllocateAddresses(request));

        log.info("Tencent create eip request sent. EipId={}. RequestId={}.",
                response.getAddressSet()[0], response.getRequestId());

        return response.getAddressSet()[0];
    }

    @Override
    public String createBandwidthPackage(CreateBandwidthPackageRequest request) {
        if(request.getBandwidthPackageCount() != null && request.getBandwidthPackageCount() > 1)
            throw new StratoException("Do not create multiple bandwidth packages through this api.");

        var response = tryInvoke(() -> buildVpcClient().CreateBandwidthPackage(request));

        log.info("Tencent create bwp request sent. BwpId={}. RequestId={}.",
                response.getBandwidthPackageId(), response.getRequestId());

        return response.getBandwidthPackageId();
    }

    @Override
    public void deleteBandwidthPackage(String packageId) {
        DeleteBandwidthPackageRequest request = new DeleteBandwidthPackageRequest();
        request.setBandwidthPackageId(packageId);

        var response = tryInvoke(() -> buildVpcClient().DeleteBandwidthPackage(request));

        log.info("Tencent delete bwp request sent. BwpId={}. RequestId={}.",
                packageId, response.getRequestId());
    }

    @Override
    public void addBandwidthPackageResource(String packageId, String resourceId, String resourceType) {
        AddBandwidthPackageResourcesRequest request = new AddBandwidthPackageResourcesRequest();
        request.setBandwidthPackageId(packageId);
        request.setResourceType(resourceType);
        request.setResourceIds(new String[]{resourceId});
        var response = tryInvoke(() -> buildVpcClient().AddBandwidthPackageResources(request));

        log.info("Tencent add bwp resource request sent. BwpId={}. RequestId={}.",
                packageId, response.getRequestId());
    }

    @Override
    public void removeBandwidthPackageResource(String packageId, String resourceId, String resourceType) {
        RemoveBandwidthPackageResourcesRequest request = new RemoveBandwidthPackageResourcesRequest();
        request.setBandwidthPackageId(packageId);
        request.setResourceType(resourceType);
        request.setResourceIds(new String[]{resourceId});

        var response = tryInvoke(() -> buildVpcClient().RemoveBandwidthPackageResources(request));

        log.info("Tencent remove bwp resource request sent. BwpId={}. RequestId={}.",
                packageId, response.getRequestId());
    }


    @Override
    public void associateNicToSecurityGroup(String nicId, String securityGroupId) {
        var request = new AssociateNetworkInterfaceSecurityGroupsRequest();
        request.setNetworkInterfaceIds(new String[]{nicId});
        request.setSecurityGroupIds(new String[]{securityGroupId});

        var response = tryInvoke(() -> buildVpcClient().AssociateNetworkInterfaceSecurityGroups(request));

        log.info("Tencent associate nic to security group request sent. NicId={}. SecurityGroupId={}. RequestId={}.",
                nicId, securityGroupId, response.getRequestId());
    }

    @Override
    public void disassociateNicFromSecurityGroup(String nicId, String securityGroupId) {
        var request = new DisassociateNetworkInterfaceSecurityGroupsRequest();
        request.setNetworkInterfaceIds(new String[]{nicId});
        request.setSecurityGroupIds(new String[]{securityGroupId});
        var response = tryInvoke(() -> buildVpcClient().DisassociateNetworkInterfaceSecurityGroups(request));

        log.info("Tencent disassociate nic from security group request sent. NicId={}. SecurityGroupId={}. RequestId={}.",
                nicId, securityGroupId, response.getRequestId());
    }


    @Override
    public void associateEipToNic(String eipId, String nicId, String privateIp) {
        AssociateAddressRequest request = new AssociateAddressRequest();
        request.setAddressId(eipId);
        request.setNetworkInterfaceId(nicId);
        request.setPrivateIpAddress(privateIp);

        var response = tryInvoke(() -> buildVpcClient().AssociateAddress(request));

        log.info("Tencent associate eip to nic request sent. EipId={}. NicId={}. RequestId={}.",
                eipId, nicId, response.getRequestId());

        if(Utils.isNotBlank(response.getTaskId()))
            waitForEipTask(Long.parseLong(response.getTaskId()));
    }

    @Override
    public void disassociateEipFromNic(String eipId) {
        DisassociateAddressRequest request = new DisassociateAddressRequest();
        request.setAddressId(eipId);

        var response = tryInvoke(() -> buildVpcClient().DisassociateAddress(request));

        log.info("Tencent disassociate eip request sent. EipId={}. RequestId={}.",
                eipId, response.getRequestId());

        if(Utils.isNotBlank(response.getTaskId()))
            waitForEipTask(Long.parseLong(response.getTaskId()));
    }

    private void waitForEipTask(Long taskId){
        DescribeTaskResultRequest request = new DescribeTaskResultRequest();
        request.setTaskId(taskId);

        VpcClient client = buildVpcClient();

        int count = 0;

        while (count < 30) {
            String status = tryInvoke(() -> client.DescribeTaskResult(request)).getResult();

            count++;

            switch (status){
                case "SUCCESS" -> {
                    log.info("Tencent eip task {} succeeded.", taskId);
                    return;
                }
                case "FAILED" -> {
                    log.error("Tencent eip task {} failed.", taskId);
                    return;
                }
                case "RUNNING" -> log.warn("Tencent eip task {} is still running.", taskId);
                default -> log.error("Unknown eip task status {}.", status);
            }

            SleepUtil.sleep(10);
        }

        log.error("Waited too long for tencent eip task {}, there might be a problem.", taskId);
    }


    @Override
    public Price inquiryPriceResetInstance(InquiryPriceResetInstanceRequest request){
        return tryInvoke(() -> buildCvmClient().InquiryPriceResetInstance(request)).getPrice();
    }

    @Override
    public Price inquiryPriceRenewInstance(InquiryPriceRenewInstancesRequest request) {
        return tryInvoke(() -> buildCvmClient().InquiryPriceRenewInstances(request)).getPrice();
    }

    @Override
    public Price inquiryPriceRunInstance(InquiryPriceRunInstancesRequest inquiry) {
        return tryInvoke(() -> buildCvmClient().InquiryPriceRunInstances(inquiry)).getPrice();
    }

    @Override
    public PrepayPrice inquiryPriceRenewDisk(InquiryPriceRenewDisksRequest request) {
        return tryInvoke(() -> buildCbsClient().InquiryPriceRenewDisks(request)).getDiskPrice();
    }

    @Override
    public PrepayPrice inquiryPriceResizeDisk(InquiryPriceResizeDiskRequest request) {
        return tryInvoke(() -> buildCbsClient().InquiryPriceResizeDisk(request)).getDiskPrice();
    }

    @Override
    public com.tencentcloudapi.cbs.v20170312.models.Price inquiryPriceCreateDisk(InquiryPriceCreateDisksRequest inquiry) {
        return tryInvoke(() -> buildCbsClient().InquiryPriceCreateDisks(inquiry)).getDiskPrice();
    }

    @Override
    public InstanceRefund inquiryPriceTerminateInstance(String instanceId) {
        var request = new InquiryPriceTerminateInstancesRequest();
        request.setInstanceIds(new String[]{instanceId});
        return tryInvoke(() -> buildCvmClient().InquiryPriceTerminateInstances(request)).getInstanceRefundsSet()[0];
    }

    @Override
    public Price inquiryPriceResetInstanceType(InquiryPriceResetInstancesTypeRequest request) {
        return tryInvoke(() -> buildCvmClient().InquiryPriceResetInstancesType(request)).getPrice();
    }

    @Override
    public String describeInstanceConsoleUrl(String instanceId) {
        DescribeInstanceVncUrlRequest request = new DescribeInstanceVncUrlRequest();
        request.setInstanceId(instanceId);
        return tryInvoke(() -> buildCvmClient().DescribeInstanceVncUrl(request)).getInstanceVncUrl();
    }


    @Override
    public Optional<LoadBalancer> describeLoadBalancer(String lbId) {
        DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest();
        request.setLoadBalancerIds(new String[]{lbId});
        return describeLoadBalancers(request).stream().findAny();
    }

    @Override
    public List<LoadBalancer> describeLoadBalancers(DescribeLoadBalancersRequest request){
        return queryAll(
                () -> buildClbClient().DescribeLoadBalancers(request),
                DescribeLoadBalancersResponse::getLoadBalancerSet,
                DescribeLoadBalancersResponse::getTotalCount,
                request::setOffset,
                request::setLimit
        );
    }

    @Override
    public String createLoadBalancer(CreateLoadBalancerRequest request) {
        if(request.getNumber() != null && request.getNumber() > 1)
            throw new StratoException("Do not create multiple load balancers through this api.");

        var response = tryInvoke(() -> buildClbClient().CreateLoadBalancer(request));
        String loadBalancerId = response.getLoadBalancerIds()[0];

        log.info("Tencent create load balancer request sent. LoadBalancerId={}. RequestId={}.",
                loadBalancerId, response.getRequestId());

        return loadBalancerId;
    }

    @Override
    public void deleteLoadBalancer(String loadBalancerId) {
        DeleteLoadBalancerRequest request = new DeleteLoadBalancerRequest();
        request.setLoadBalancerIds(new String[]{loadBalancerId});
        var response = tryInvoke(() -> buildClbClient().DeleteLoadBalancer(request));
        log.info("Tencent delete load balancer request sent. LoadBalancerId={}. RequestId={}.",
                loadBalancerId, response.getRequestId());
    }

    @Override
    public Optional<TencentListener> describeListener(TencentListenerId listenerId){
        DescribeListenersRequest request = new DescribeListenersRequest();
        request.setLoadBalancerId(listenerId.lbId());
        request.setListenerIds(new String[]{listenerId.listenerId()});

        Listener[] listeners = tryInvoke(() -> buildClbClient().DescribeListeners(request)).getListeners();

        if(Utils.isEmpty(listeners))
            return Optional.empty();

        return Optional.of(new TencentListener(listenerId.lbId(), listeners[0]));
    }

    @Override
    public List<TencentListener> describeListeners(){
        List<TencentListener> result = new ArrayList<>();

        List<LoadBalancer> loadBalancers = describeLoadBalancers(new DescribeLoadBalancersRequest());

        for (LoadBalancer loadBalancer : loadBalancers) {
            DescribeListenersRequest request = new DescribeListenersRequest();
            request.setLoadBalancerId(loadBalancer.getLoadBalancerId());
            Listener[] listeners = tryInvoke(() -> buildClbClient().DescribeListeners(request)).getListeners();

            if(Utils.isEmpty(listeners))
                continue;

            for (Listener listener : listeners) {
                result.add(new TencentListener(loadBalancer.getLoadBalancerId(), listener));
            }
        }

        return result;
    }

    private void waitForClbTask(String requestId){
        DescribeTaskStatusRequest request = new DescribeTaskStatusRequest();
        request.setTaskId(requestId);

        ClbClient client = buildClbClient();

        int count = 0;

        while (count < 30) {
            var response = tryInvoke(() -> client.DescribeTaskStatus(request));
            int status = response.getStatus().intValue();

            count++;

            switch (status){
                case 0 -> {
                    log.info("Tencent clb task {} succeeded.", requestId);
                    return;
                }
                case 1 -> {
                    log.error("Tencent clb task {} failed.", requestId);
                    return;
                }
                case 2 -> log.warn("Tencent clb task {} is still running.", requestId);
                default -> log.error("Unknown clb task status {}.", status);
            }

            SleepUtil.sleep(10);
        }

        log.error("Waited too long for tencent clb task {}, there might be a problem.", requestId);
    }

    @Override
    public String createListener(CreateListenerRequest request) {
        CreateListenerResponse response = tryInvoke(() -> buildClbClient().CreateListener(request));
        log.info("Tencent create lb listener request sent. ListenerId={}. RequestId={}.",
                response.getListenerIds()[0], response.getRequestId());
        waitForClbTask(response.getRequestId());
        return response.getListenerIds()[0];
    }

    @Override
    public void deleteListener(TencentListenerId listenerId) {
        DeleteListenerRequest request = new DeleteListenerRequest();
        request.setLoadBalancerId(listenerId.lbId());
        request.setListenerId(listenerId.listenerId());

        var response = tryInvoke(() -> buildClbClient().DeleteListener(request));
        log.info("Tencent delete lb listener request sent. ListenerId={}. RequestId={}.",
                listenerId, response.getRequestId());

        waitForClbTask(response.getRequestId());
    }


    @Override
    public Optional<TencentBackend> describeBackend(TencentInstanceBackendId backendId) {
        DescribeTargetsRequest request = new DescribeTargetsRequest();
        request.setLoadBalancerId(backendId.lbId());
        request.setListenerIds(new String[]{backendId.listenerId()});

        ListenerBackend[] listeners = tryInvoke(() -> buildClbClient().DescribeTargets(request)).getListeners();

        if(Utils.isEmpty(listeners))
            return Optional.empty();

        ListenerBackend listener = listeners[0];

        List<TencentBackend> backends = getListenerBackends(backendId.lbId(), listener);

        return backends.stream().filter(
                backend -> Objects.equals(backend.backend().getInstanceId(), backendId.instanceId())
        ).findAny();
    }

    @Override
    public Optional<TencentBackend> describeBackend(TencentNicBackendId backendId) {
        Optional<ListenerBackend> listenerBackend = describeListenerBackend(
                new TencentListenerId(backendId.lbId(), backendId.listenerId())
        );

        if(listenerBackend.isEmpty())
            return Optional.empty();

        List<TencentBackend> backends = getListenerBackends(backendId.lbId(), listenerBackend.get());

        return backends.stream().filter(
                backend -> Objects.equals(backend.backend().getPrivateIpAddresses()[0], backendId.ip())
        ).findAny();
    }

    @Override
    public List<TencentBackend> describeBackends() {
        List<TencentBackend> result = new ArrayList<>();

        List<LoadBalancer> loadBalancers = describeLoadBalancers(new DescribeLoadBalancersRequest());

        for (LoadBalancer loadBalancer : loadBalancers) {
            DescribeTargetsRequest request = new DescribeTargetsRequest();
            request.setLoadBalancerId(loadBalancer.getLoadBalancerId());

            ListenerBackend[] listeners = tryInvoke(() -> buildClbClient().DescribeTargets(request)).getListeners();

            if(Utils.isEmpty(listeners))
                continue;

            for (ListenerBackend listener : listeners) {
                List<TencentBackend> backends = getListenerBackends(loadBalancer.getLoadBalancerId(), listener);
                result.addAll(backends);
            }
        }

        return result;
    }

    private static List<TencentBackend> getListenerBackends(String loadBalancerId,
                                                            ListenerBackend listener) {
        List<TencentBackend> result = new ArrayList<>();

        if(Utils.isNotEmpty(listener.getRules()))
            for (RuleTargets rule : listener.getRules())
                if(Utils.isNotEmpty(rule.getTargets()))
                    for (Backend target : rule.getTargets())
                        result.add(new TencentBackend(loadBalancerId, listener.getListenerId(), target));

        if(Utils.isNotEmpty(listener.getTargets()))
            for (Backend target : listener.getTargets())
                result.add(new TencentBackend(loadBalancerId, listener.getListenerId(), target));

        return result;
    }

    @Override
    public Optional<TencentL7Rule> describeL7Rule(TencentL7RuleId ruleId) {
        TencentListenerId listenerId = new TencentListenerId(ruleId.lbId(), ruleId.listenerId());
        List<TencentL7Rule> rules = describeL7Rules(listenerId);
        return rules.stream().filter(
                rule -> Objects.equals(rule.ruleId(), ruleId)
        ).findAny();
    }

    @Override
    public List<TencentL7Rule> describeL7Rules(TencentListenerId listenerId){
        Optional<TencentListener> listener = describeListener(listenerId);
        if(listener.isEmpty())
            return List.of();
        RuleOutput[] rules = listener.get().listener().getRules();
        if(Utils.isEmpty(rules))
            return List.of();
        return Arrays.stream(rules).map(
               ruleOutput ->  new TencentL7Rule(
                       new TencentL7RuleId(
                               listenerId.lbId(),
                               listenerId.listenerId(),
                               ruleOutput.getLocationId()
                       ),
                       ruleOutput
               )
        ).toList();
    }

    @Override
    public List<TencentL7Rule> describeL7Rules() {
        List<TencentListener> listeners = describeListeners();
        List<TencentL7Rule> result = new ArrayList<>();
        for (TencentListener listener : listeners) {
            TencentListenerId listenerId = new TencentListenerId(
                    listener.loadBalancerId(),
                    listener.listener().getListenerId()
            );
            result.addAll(describeL7Rules(listenerId));
        }
        return result;
    }


    @Override
    public String createRule(CreateRuleRequest request) {
        if(request.getRules().length > 1)
            throw new StratoException("Do not create multiple rules throw this api.");
        CreateRuleResponse response = tryInvoke(() -> buildClbClient().CreateRule(request));

        log.info("Tencent create L7 rule request sent. RequestId={}. LocationId={}.",
                response.getRequestId(), response.getLocationIds()[0]);

        waitForClbTask(response.getRequestId());

        return response.getLocationIds()[0];
    }

    @Override
    public Optional<ListenerBackend> describeListenerBackend(TencentListenerId listenerId) {
        DescribeTargetsRequest request = new DescribeTargetsRequest();
        request.setLoadBalancerId(listenerId.lbId());
        request.setListenerIds(new String[]{listenerId.listenerId()});

        ListenerBackend[] listeners = tryInvoke(() -> buildClbClient().DescribeTargets(request)).getListeners();

        if(Utils.isEmpty(listeners))
            return Optional.empty();

        return Optional.of(listeners[0]);
    }


    @Override
    public void registerTarget(RegisterTargetsRequest request) {
        var response = tryInvoke(() -> buildClbClient().RegisterTargets(request));

        log.info("Tencent register backend target request sent. RequestId={}.", response.getRequestId());

        waitForClbTask(response.getRequestId());
    }

    @Override
    public void deregisterTarget(DeregisterTargetsRequest request) {
        var response = tryInvoke(() -> buildClbClient().DeregisterTargets(request));

        log.info("Tencent deregister backend target request sent. RequestId={}.", response.getRequestId());

        waitForClbTask(response.getRequestId());
    }


    @Override
    public com.tencentcloudapi.clb.v20180317.models.Price inquiryPriceCreateLoadBalancer(
            InquiryPriceCreateLoadBalancerRequest request
    ) {
        return tryInvoke(() -> buildClbClient().InquiryPriceCreateLoadBalancer(request)).getPrice();
    }

    @Override
    public Optional<Certificates> describeCert(String certId) {
        DescribeCertificatesRequest request = new DescribeCertificatesRequest();
        request.setSearchKey(certId);

        return describeCerts(request).stream().filter(
                cert -> cert.getCertificateId().equals(certId)
        ).findAny();
    }


    @Override
    public List<Certificates> describeCerts(DescribeCertificatesRequest request) {
        return queryAll(
                () -> buildSslClient().DescribeCertificates(request),
                DescribeCertificatesResponse::getCertificates,
                DescribeCertificatesResponse::getTotalCount,
                request::setOffset,
                request::setLimit
        );
    }

    @Override
    public String applyFreeCertificate(ApplyCertificateRequest request) {
        var response = tryInvoke(() -> buildSslClient().ApplyCertificate(request));

        log.info("Tencent apply free certificate request sent. CertId={}. RequestId={}.",
                response.getCertificateId(), response.getRequestId());

        return response.getCertificateId();
    }

    @Override
    public String createCertificate(CreateCertificateRequest request) {
        var response = tryInvoke(() -> buildSslClient().CreateCertificate(request));

        log.info("Tencent create certificate request sent. CertId={}. RequestId={}.",
                response.getCertificateIds()[0], response.getRequestId());

        return response.getCertificateIds()[0];
    }


    @Override
    public void deleteCertificate(String certId) {
        DeleteCertificateRequest request = new DeleteCertificateRequest();
        request.setCertificateId(certId);
        request.setIsCheckResource(true);

        var response = tryInvoke(() -> buildSslClient().DeleteCertificate(request));

        log.info("Tencent delete certificate request sent. CertId={}. RequestId={}.",
                certId, response.getRequestId());
    }


    @Override
    public void deleteRule(DeleteRuleRequest request) {
        String requestId = tryInvoke(() -> buildClbClient().DeleteRule(request)).getRequestId();
        log.info("Tencent delete rule request sent. RequestId={}.", requestId);
        waitForClbTask(requestId);
    }

    @Override
    public void deleteEip(String eipId) {
        ReleaseAddressesRequest request = new ReleaseAddressesRequest();
        request.setAddressIds(new String[]{eipId});

        ReleaseAddressesResponse response = tryInvoke(() -> buildVpcClient().ReleaseAddresses(request));

        log.info("Tencent delete eip request sent. RequestId={}.", response.getRequestId());

        if(Utils.isNotBlank(response.getTaskId()))
            waitForEipTask(Long.valueOf(response.getTaskId()));
    }

    @Override
    public GetMonitorDataResponse getMonitorData(GetMonitorDataRequest request){
        return tryInvoke(() -> buildMonitorClient().GetMonitorData(request));
    }

    @Override
    public GetUserAppIdResponse getUserAppId(){
        GetUserAppIdRequest request = new GetUserAppIdRequest();
        return tryInvoke(() -> buildCamClient().GetUserAppId(request));
    }

    @Override
    public Optional<InvocationTask> describeInvocationTask(String taskId) {
        DescribeInvocationTasksRequest request = new DescribeInvocationTasksRequest();
        request.setHideOutput(false);
        request.setInvocationTaskIds(new String[]{taskId});
        return describeInvocationTasks(request).stream().findAny();
    }

    @Override
    public List<InvocationTask> describeInvocationTasks(DescribeInvocationTasksRequest request){
        return queryAll(
                () -> buildTatClient().DescribeInvocationTasks(request),
                DescribeInvocationTasksResponse::getInvocationTaskSet,
                DescribeInvocationTasksResponse::getTotalCount,
                request::setOffset,
                request::setLimit
        );
    }

    @Override
    public List<Invocation> describeInvocations(DescribeInvocationsRequest request){
        return queryAll(
                () -> buildTatClient().DescribeInvocations(request),
                DescribeInvocationsResponse::getInvocationSet,
                DescribeInvocationsResponse::getTotalCount,
                request::setOffset,
                request::setLimit
        );
    }

    @Override
    public Optional<Invocation> describeInvocation(String invocationId){
        DescribeInvocationsRequest request = new DescribeInvocationsRequest();
        request.setInvocationIds(new String[]{invocationId});
        return describeInvocations(request).stream().findAny();
    }

    private Invocation getNullableInvocation(String invocationId){
        return describeInvocation(invocationId).orElse(null);
    }

    @Override
    public Invocation runCommand(RunCommandRequest request) {
        RunCommandResponse response = tryInvoke(() -> buildTatClient().RunCommand(request));

        log.info("Tencent run command request sent, instanceId={}, requestId={}.",
                request.getInstanceIds(), response.getRequestId());

        Invocation invocation;

        Set<String> waitingStatuses = Set.of("PENDING", "RUNNING");

        String invId = response.getInvocationId();

        int triedTimes = 0;

        while ((invocation = getNullableInvocation(invId)) == null ||
                waitingStatuses.contains(invocation.getInvocationStatus())){
            if(triedTimes > 100) {
                log.warn("Max tries exceeded for awaiting tencent command invocation.");
                break;
            }
            triedTimes++;
            SleepUtil.sleep(3);
        }

        if(invocation == null)
            throw new StratoException("Cannot find tencent invocation %s".formatted(invId));


        if(waitingStatuses.contains(invocation.getInvocationStatus()))
            log.warn("Waited too long for tencent invocation {}, status remains {}",
                    invId, invocation.getInvocationStatus());

        return invocation;
    }

    @Override
    public void modifyInstancesAttribute(ModifyInstancesAttributeRequest request) {
        ModifyInstancesAttributeResponse response = tryInvoke(
                () -> buildCvmClient().ModifyInstancesAttribute(request)
        );
        log.info("Tencent modify instances attribute request sent, requestId={}", response.getRequestId());
    }

    @Override
    public void resetInstancesPassword(ResetInstancesPasswordRequest request) {
        ResetInstancesPasswordResponse response = tryInvoke(
                () -> buildCvmClient().ResetInstancesPassword(request)
        );
        log.info("Tencent reset instances password request sent, requestId={}", response.getRequestId());
    }


    @Override
    public List<Snapshot> describeSnapshots(DescribeSnapshotsRequest request){
        return queryAll(
                () -> buildCbsClient().DescribeSnapshots(request),
                DescribeSnapshotsResponse::getSnapshotSet,
                DescribeSnapshotsResponse::getTotalCount,
                request::setOffset,
                request::setLimit
        );
    }

    @Override
    public Optional<Snapshot> describeSnapshot(String snapshotId) {
        DescribeSnapshotsRequest request = new DescribeSnapshotsRequest();

        request.setSnapshotIds(new String[]{snapshotId});

        return describeSnapshots(request).stream().findAny();
    }

    @Override
    public String createSnapshot(CreateSnapshotRequest request) {
        CreateSnapshotResponse response = tryInvoke(
                () -> buildCbsClient().CreateSnapshot(request)
        );

        log.info("Tencent create snapshot request sent. RequestId={}. SnapshotId={}.",
                response.getRequestId(), response.getSnapshotId());

        return response.getSnapshotId();
    }

    @Override
    public void deleteSnapshot(String snapshotId){
        DeleteSnapshotsRequest request = new DeleteSnapshotsRequest();
        request.setSnapshotIds(new String[]{snapshotId});
        DeleteSnapshotsResponse response = tryInvoke(
                () -> buildCbsClient().DeleteSnapshots(request)
        );

        log.info("Tencent delete snapshot request sent. RequestId={}. SnapshotId={}.",
                response.getRequestId(), snapshotId);
    }

    @Override
    public void rollbackToSnapshot(String diskId,
                                   String snapshotId,
                                   Boolean autoStop,
                                   Boolean autoStart){
        ApplySnapshotRequest request = new ApplySnapshotRequest();

        request.setDiskId(diskId);
        request.setSnapshotId(snapshotId);
        request.setAutoStopInstance(autoStop);
        request.setAutoStartInstance(autoStart);

        ApplySnapshotResponse response = tryInvoke(
                () -> buildCbsClient().ApplySnapshot(request)
        );

        log.info("Tencent apply snapshot request sent. RequestId={}. SnapshotId={}.",
                response.getRequestId(), snapshotId);
    }


    @Override
    public List<Event> describeEvents(List<String> eventNames,
                                      String resourceType,
                                      String resourceId,
                                      LocalDateTime startTime) {
        DescribeEventsRequest request = new DescribeEventsRequest();

        List<LookupAttribute> attributes = new ArrayList<>();
        if(Utils.isNotEmpty(eventNames)){
            for (String eventName : eventNames) {
                LookupAttribute attribute = new LookupAttribute();
                attribute.setAttributeKey("EventName");
                attribute.setAttributeValue(eventName);
                attributes.add(attribute);
            }
        }

        if(Utils.isNotBlank(resourceType)){
            LookupAttribute attribute = new LookupAttribute();
            attribute.setAttributeKey("ResourceType");
            attribute.setAttributeValue(resourceType);
            attributes.add(attribute);
        }

        if(Utils.isNotBlank(resourceId)){
            LookupAttribute attribute = new LookupAttribute();
            attribute.setAttributeKey("ResourceId");
            attribute.setAttributeValue(resourceId);
            attributes.add(attribute);
        }

        LookupAttribute actionTypeAttribute = new LookupAttribute();

        actionTypeAttribute.setAttributeKey("ActionType");
        actionTypeAttribute.setAttributeValue("Write");
        attributes.add(actionTypeAttribute);

        LocalDateTime endTime = LocalDateTime.now().minusSeconds(5L);
        LocalDateTime earliestStartTime = endTime.minusDays(30L).plusSeconds(1L);

        if(earliestStartTime.isAfter(startTime))
            startTime = earliestStartTime;

        request.setLookupAttributes(attributes.toArray(LookupAttribute[]::new));
        request.setStartTime(startTime.atZone(TimeUtil.BEIJING_ZONE_ID).toEpochSecond());
        request.setEndTime(endTime.atZone(TimeUtil.BEIJING_ZONE_ID).toEpochSecond());
        request.setMaxResults(50L);

        boolean listOver = false;
        Long nextToken = null;
        List<Event> result = new ArrayList<>();

        while (!listOver){
            try {
                request.setNextToken(nextToken);
                DescribeEventsResponse response = tryInvoke(() -> buildAuditClient().DescribeEvents(request));
                if(Utils.isEmpty(response.getEvents()))
                    break;

                result.addAll(List.of(response.getEvents()));
                listOver = response.getListOver() == null || response.getListOver();
                nextToken = response.getNextToken();
            }catch (Exception e){
                log.warn("Failed to retrieve tencent events.", e);
                break;
            }
        }

        return result;
    }


    @Override
    public List<AlarmHistory> describeAlarmHistories(String resourceId,
                                                     LocalDateTime startTime) {
        DescribeAlarmHistoriesRequest request = new DescribeAlarmHistoriesRequest();
        request.setModule("monitor");
        request.setStartTime(startTime.atZone(TimeUtil.BEIJING_ZONE_ID).toEpochSecond());
        request.setEndTime(LocalDateTime.now().atZone(TimeUtil.BEIJING_ZONE_ID).toEpochSecond());
        request.setAlarmObject(resourceId);

        return queryAllByPage(
                () -> buildMonitorClient().DescribeAlarmHistories(request),
                DescribeAlarmHistoriesResponse::getHistories,
                DescribeAlarmHistoriesResponse::getTotalCount,
                request::setPageNumber,
                request::setPageSize
        );
    }
}
