package com.stratocloud.provider.tencent.lb.listener.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.lb.listener.TencentL4ListenerHandler;
import com.stratocloud.provider.tencent.lb.listener.TencentListenerId;
import com.stratocloud.provider.tencent.lb.listener.requirements.TencentL4ListenerToInternalLbHandler;
import com.stratocloud.provider.tencent.lb.listener.requirements.TencentL4ListenerToOpenLbHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.clb.v20180317.models.CertificateInput;
import com.tencentcloudapi.clb.v20180317.models.CreateListenerRequest;
import com.tencentcloudapi.clb.v20180317.models.HealthCheck;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class TencentL4ListenerBuildHandler implements BuildResourceActionHandler {


    private final TencentL4ListenerHandler listenerHandler;

    public TencentL4ListenerBuildHandler(TencentL4ListenerHandler listenerHandler) {
        this.listenerHandler = listenerHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return listenerHandler;
    }

    @Override
    public String getTaskName() {
        return "创建四层监听器";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentL4ListenerBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentL4ListenerBuildInput input = JSON.convert(parameters, TencentL4ListenerBuildInput.class);

        Resource lb = resource.getExclusiveTarget(ResourceCategories.LOAD_BALANCER).orElseThrow(
                () -> new StratoException("LB not found when creating listener.")
        );

        CreateListenerRequest request = new CreateListenerRequest();
        request.setLoadBalancerId(lb.getExternalId());
        request.setPorts(new Long[]{input.getPort()});
        request.setProtocol(input.getProtocol());
        request.setListenerNames(new String[]{resource.getName()});

        if(input.getEnableHealthCheck() != null){
            HealthCheck healthCheck = new HealthCheck();

            if(input.getEnableHealthCheck()){
                healthCheck.setHealthSwitch(1L);
                healthCheck.setTimeOut(input.getHealthCheckTimeout());
                healthCheck.setIntervalTime(input.getHealthCheckInterval());
                healthCheck.setHealthNum(input.getHealthyNumber());
                healthCheck.setUnHealthNum(input.getUnhealthyNumber());
                healthCheck.setHttpCode(input.getHttpCode());

                if(Utils.isNotBlank(input.getHttpCheckPath()))
                    healthCheck.setHttpCheckPath(input.getHttpCheckPath());

                if(Utils.isNotBlank(input.getHttpCheckDomain()))
                    healthCheck.setHttpCheckDomain(input.getHttpCheckDomain());

                if(Utils.isNotBlank(input.getHttpCheckMethod()))
                    healthCheck.setHttpCheckMethod(input.getHttpCheckMethod());

                healthCheck.setCheckPort(input.getCheckPort());

                if(Utils.isNotBlank(input.getCheckType()))
                    healthCheck.setCheckType(input.getCheckType());

                if(Utils.isNotBlank(input.getHttpVersion()))
                    healthCheck.setHttpVersion(input.getHttpVersion());

                healthCheck.setSourceIpType(input.getSourceIpType());
            }else {
                healthCheck.setHealthSwitch(0L);
            }

            request.setHealthCheck(healthCheck);
        }


        if(input.getSessionExpireTime() != null)
            request.setSessionExpireTime(input.getSessionExpireTime());

        if(Utils.isNotBlank(input.getScheduler()))
            request.setScheduler(input.getScheduler());


        if(Utils.isNotBlank(input.getSessionType()))
            request.setSessionType(input.getSessionType());


        if(input.getDeregisterTargetRst() != null)
            request.setDeregisterTargetRst(input.getDeregisterTargetRst());

        if(input.getMaxConn() != null)
            request.setMaxConn(input.getMaxConn());

        if(input.getMaxCps() != null)
            request.setMaxCps(input.getMaxCps());

        if(input.getIdleConnectTimeout() != null)
            request.setIdleConnectTimeout(input.getIdleConnectTimeout());

        if(input.getEnableSnat() != null && input.getEnableSnat())
            request.setSnatEnable(input.getEnableSnat());

        if(isTcpSsl(input.getProtocol())){
            Resource cert = resource.getExclusiveTarget(ResourceCategories.SERVER_CERT).orElseThrow(
                    () -> new StratoException("Cert not found when creating L4 TCP_SSL listener.")
            );

            CertificateInput certificateInput = new CertificateInput();
            certificateInput.setCertId(cert.getExternalId());
            certificateInput.setSSLMode("UNIDIRECTIONAL");
            request.setCertificate(certificateInput);
        }

        TencentCloudProvider provider = (TencentCloudProvider) listenerHandler.getProvider();
        String listenerId = provider.buildClient(account).createListener(request);

        resource.setExternalId(new TencentListenerId(lb.getExternalId(), listenerId).toString());
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource,
                                                             Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        Optional<Resource> cert = resource.getExclusiveTarget(ResourceCategories.SERVER_CERT);

        TencentL4ListenerBuildInput input = JSON.convert(parameters, TencentL4ListenerBuildInput.class);

        String protocol = input.getProtocol();

        if(isTcpSsl(protocol) && cert.isEmpty())
            throw new BadCommandException("选择TCP_SSL协议需要绑定SSL证书");

        if(!isTcpSsl(protocol) && cert.isPresent())
            throw new BadCommandException("非TCP_SSL协议不能选择SSL证书");

        List<Resource> targets = resource.getRequirementTargets(ResourceCategories.LOAD_BALANCER);

        if(Utils.isEmpty(targets))
            throw new BadCommandException("请指定一个LB实例");

        if(targets.size() > 1)
            throw new BadCommandException("每个监听器只能指定一个LB实例");

    }

    private static boolean isTcpSsl(String protocol) {
        return Objects.equals("TCP_SSL", protocol);
    }

    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(
                TencentL4ListenerToInternalLbHandler.TYPE_ID,
                TencentL4ListenerToOpenLbHandler.TYPE_ID
        );
    }
}
