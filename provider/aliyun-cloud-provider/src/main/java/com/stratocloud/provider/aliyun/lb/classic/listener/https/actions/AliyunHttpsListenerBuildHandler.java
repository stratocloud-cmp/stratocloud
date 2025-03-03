package com.stratocloud.provider.aliyun.lb.classic.listener.https.actions;

import com.aliyun.slb20140515.models.CreateLoadBalancerHTTPSListenerRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerId;
import com.stratocloud.provider.aliyun.lb.classic.common.AliyunListenerToAclInput;
import com.stratocloud.provider.aliyun.lb.classic.listener.https.AliyunHttpsListenerHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.https.requrements.AliyunHttpsListenerToInternetClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.listener.https.requrements.AliyunHttpsListenerToIntranetClbHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class AliyunHttpsListenerBuildHandler implements BuildResourceActionHandler {

    private final AliyunHttpsListenerHandler listenerHandler;

    public AliyunHttpsListenerBuildHandler(AliyunHttpsListenerHandler listenerHandler) {
        this.listenerHandler = listenerHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return listenerHandler;
    }

    @Override
    public String getTaskName() {
        return "创建监听器";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return AliyunHttpsListenerBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        var input = JSON.convert(parameters, AliyunHttpsListenerBuildInput.class);

        CreateLoadBalancerHTTPSListenerRequest request = new CreateLoadBalancerHTTPSListenerRequest();

        resolveBasicOptions(resource, input, request);

        resolveSessionOptions(input, request);

        resolveForwardOptions(input, request);

        resolveHealthCheckOptions(input, request);

        resolveHttpsOptions(resource, input, request);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) listenerHandler.getProvider();

        provider.buildClient(account).clb().createHttpsListener(request);
    }

    private void resolveHttpsOptions(Resource resource,
                                     AliyunHttpsListenerBuildInput input,
                                     CreateLoadBalancerHTTPSListenerRequest request) {
        Optional<Resource> serverCert = resource.getExclusiveTarget(ResourceCategories.SERVER_CERT);

        serverCert.ifPresent(value -> request.setServerCertificateId(value.getExternalId()));

        request.setEnableHttp2(input.getEnableHttp2());

        if(Utils.isNotBlank(input.getTlsCipherPolicy()))
            request.setTLSCipherPolicy(input.getTlsCipherPolicy());
    }

    private static void resolveBasicOptions(Resource resource,
                                            AliyunHttpsListenerBuildInput input,
                                            CreateLoadBalancerHTTPSListenerRequest request) {
        Resource lb = resource.getExclusiveTarget(ResourceCategories.LOAD_BALANCER).orElseThrow(
                () -> new StratoException("LB not found when creating listener.")
        );

        Optional<Resource> backendGroup = resource.getExclusiveTarget(
                ResourceCategories.LOAD_BALANCER_BACKEND_GROUP
        );

        Optional<Relationship> aclRequirement = resource.getExclusiveRequirements().stream().filter(
                rel -> rel.getTarget().isCategory(ResourceCategories.LOAD_BALANCER_ACL)
        ).findAny();

        request.setLoadBalancerId(lb.getExternalId());

        backendGroup.ifPresent(value -> request.setVServerGroupId(value.getExternalId()));

        if (aclRequirement.isPresent()) {
            var aclInput = JSON.convert(aclRequirement.get().getProperties(), AliyunListenerToAclInput.class);

            Resource acl = aclRequirement.get().getTarget();

            request.setAclId(acl.getExternalId());
            request.setAclStatus("on");

            if(Utils.isNotBlank(aclInput.getAclType()))
                request.setAclType(aclInput.getAclType());
        }

        request.setListenerPort(input.getPort());
        request.setBackendServerPort(input.getBackendPort());
        request.setBandwidth(input.getBandwidth());
        request.setScheduler(input.getScheduler());

        request.setIdleTimeout(input.getIdleTimeout());
        request.setRequestTimeout(input.getRequestTimeout());

        request.setGzip(input.getGzip());
        request.setDescription(resource.getDescription());

        AliyunListenerId listenerId = new AliyunListenerId(
                lb.getExternalId(),
                "https",
                input.getPort().toString()
        );
        resource.setExternalId(listenerId.toString());
    }

    private static void resolveHealthCheckOptions(AliyunHttpsListenerBuildInput input,
                                                  CreateLoadBalancerHTTPSListenerRequest request) {
        boolean enableHealthCheck = input.getEnableHealthCheck()!=null ? input.getEnableHealthCheck() : false;
        if(enableHealthCheck){
            request.setHealthCheck("on");

            request.setHealthCheckConnectPort(input.getHealthCheckConnectPort());
            request.setHealthCheckDomain(input.getHealthCheckDomain());

            if(Utils.isNotEmpty(input.getHealthCheckHttpCode()))
                request.setHealthCheckHttpCode(String.join(",", input.getHealthCheckHttpCode()));

            request.setHealthCheckInterval(input.getHealthCheckInterval());
            request.setHealthCheckMethod(input.getHealthCheckMethod());
            request.setHealthCheckTimeout(input.getHealthCheckTimeout());
            request.setHealthCheckURI(input.getHealthCheckUri());
            request.setHealthyThreshold(input.getHealthyThreshold());
            request.setUnhealthyThreshold(input.getUnhealthyThreshold());
        }else {
            request.setHealthCheck("off");
        }
    }

    private static void resolveForwardOptions(AliyunHttpsListenerBuildInput input,
                                              CreateLoadBalancerHTTPSListenerRequest request) {
        request.setXForwardedFor(input.getXForwardedFor());
        request.setXForwardedFor_ClientSrcPort(input.getXForwardedFor_ClientSrcPort());
        request.setXForwardedFor_SLBID(input.getXForwardedFor_SLBID());
        request.setXForwardedFor_SLBIP(input.getXForwardedFor_SLBIP());
        request.setXForwardedFor_SLBPORT(input.getXForwardedFor_SLBPORT());
        request.setXForwardedFor_proto(input.getXForwardedFor_proto());
    }

    private static void resolveSessionOptions(AliyunHttpsListenerBuildInput input,
                                              CreateLoadBalancerHTTPSListenerRequest request) {
        request.setStickySession(input.getStickySession());
        if(Objects.equals("on", input.getStickySession())){
            request.setStickySessionType(input.getStickySessionType());

            if(Objects.equals("insert", input.getStickySessionType()))
                request.setCookieTimeout(input.getCookieTimeout());
            else if(Objects.equals("server", input.getStickySessionType()))
                request.setCookie(input.getCookie());
        }
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        List<Resource> targets = resource.getRequirementTargets(ResourceCategories.LOAD_BALANCER);

        if(Utils.isEmpty(targets))
            throw new BadCommandException("请指定一个LB实例");

        if(targets.size() > 1)
            throw new BadCommandException("每个监听器只能指定一个LB实例");
    }

    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(
                AliyunHttpsListenerToIntranetClbHandler.TYPE_ID,
                AliyunHttpsListenerToInternetClbHandler.TYPE_ID
        );
    }
}
