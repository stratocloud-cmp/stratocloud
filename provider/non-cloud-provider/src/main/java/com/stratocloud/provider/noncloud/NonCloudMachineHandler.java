package com.stratocloud.provider.noncloud;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.ip.IpAddress;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.RuntimePropertiesUtil;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.guest.command.*;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class NonCloudMachineHandler extends AbstractResourceHandler implements GuestOsHandler {

    private final NonCloudProvider provider;

    public NonCloudMachineHandler(NonCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public OsType getOsType(Resource resource) {
        return JSON.convert(resource.getProperties(), NonCloudMachineBuildInput.class).getOsType();
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "NON_CLOUD_MACHINE";
    }

    @Override
    public String getResourceTypeName() {
        return "非云主机";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.NON_CLOUD_MACHINE;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return Optional.of(getExternalResource(account, externalId));
    }

    private ExternalResource getExternalResource(ExternalAccount account, String externalId) {
        IpAddress ipAddress = new IpAddress(externalId);
        boolean ipReachable = ipAddress.isReachable();
        return new ExternalResource(
                getProvider().getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                externalId,
                externalId,
                ipReachable ? ResourceState.STARTED : ResourceState.UNKNOWN
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return List.of();
    }

    @Override
    public void synchronize(Resource resource) {
        Optional<String> managementIp = RuntimePropertiesUtil.getManagementIp(resource);
        managementIp.ifPresent(resource::setExternalId);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        ExternalResource externalResource = getExternalResource(account, resource.getExternalId());
        resource.updateByExternal(externalResource);

        Optional<StratoGuestCommandExecutorFactory> factory = StratoGuestCommandExecutorFactoryRegistry.getByOsType(
                getOsTypeQuietly(resource)
        );

        if(factory.isPresent()){
            try (StratoGuestCommandExecutor executor = factory.get().createExecutor(this, resource)) {
                GuestCommandResult result = executor.execute(new GuestCommand("hostname"));

                if(result.status() == GuestCommandResult.Status.SUCCESS && Utils.isNotBlank(result.output())){
                    RuntimeProperty hostnameProperty = RuntimeProperty.ofDisplayInList(
                            "hostname", "主机名", result.output(), result.output()
                    );
                    resource.addOrUpdateRuntimeProperty(hostnameProperty);
                }
            } catch (IOException e) {
                log.warn("Failed to connect to non-cloud machine: ", e);
            }
        }
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
