package com.stratocloud.provider.tencent.lb.listener.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.lb.listener.TencentL7ListenerHandler;
import com.stratocloud.provider.tencent.lb.listener.TencentListenerId;
import com.stratocloud.provider.tencent.lb.listener.requirements.TencentL7ListenerToInternalLbHandler;
import com.stratocloud.provider.tencent.lb.listener.requirements.TencentL7ListenerToOpenLbHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.clb.v20180317.models.CertificateInput;
import com.tencentcloudapi.clb.v20180317.models.CreateListenerRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class TencentL7ListenerBuildHandler implements BuildResourceActionHandler {

    private final TencentL7ListenerHandler listenerHandler;

    public TencentL7ListenerBuildHandler(TencentL7ListenerHandler listenerHandler) {
        this.listenerHandler = listenerHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return listenerHandler;
    }

    @Override
    public String getTaskName() {
        return "创建七层监听器";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentL7ListenerBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentL7ListenerBuildInput input = JSON.convert(parameters, TencentL7ListenerBuildInput.class);

        Resource lb = resource.getExclusiveTarget(ResourceCategories.LOAD_BALANCER).orElseThrow(
                () -> new StratoException("LB not found when creating listener.")
        );

        CreateListenerRequest request = new CreateListenerRequest();
        request.setLoadBalancerId(lb.getExternalId());
        request.setPorts(new Long[]{input.getPort()});
        request.setProtocol(input.getProtocol());
        request.setListenerNames(new String[]{resource.getName()});

        if(input.getEnableSni() != null && input.getEnableSni())
            request.setSniSwitch(0L);

        if(input.getKeepAliveEnabled() != null && input.getKeepAliveEnabled())
            request.setKeepaliveEnable(1L);

        if(input.getEnableSnat() != null && input.getEnableSnat())
            request.setSnatEnable(true);

        if(isHttps(input.getProtocol())){
            Resource cert = resource.getExclusiveTarget(ResourceCategories.SERVER_CERT).orElseThrow(
                    () -> new StratoException("Cert not found when creating L7 HTTPS listener.")
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
        TencentL7ListenerBuildInput input = JSON.convert(parameters, TencentL7ListenerBuildInput.class);

        Optional<Resource> cert = resource.getExclusiveTarget(ResourceCategories.SERVER_CERT);

        if(isHttps(input.getProtocol()) && cert.isEmpty())
            throw new BadCommandException("选择HTTPS协议必须绑定SSL证书");

        if(!isHttps(input.getProtocol()) && cert.isPresent())
            throw new BadCommandException("非HTTPS协议不能选择SSL证书");

        List<Resource> targets = resource.getRequirementTargets(ResourceCategories.LOAD_BALANCER);

        if(Utils.isEmpty(targets))
            throw new BadCommandException("请指定一个LB实例");

        if(targets.size() > 1)
            throw new BadCommandException("每个监听器只能指定一个LB实例");
    }

    private static boolean isHttps(String protocol) {
        return Objects.equals(protocol, "HTTPS");
    }

    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(
                TencentL7ListenerToInternalLbHandler.TYPE_ID,
                TencentL7ListenerToOpenLbHandler.TYPE_ID
        );
    }
}
