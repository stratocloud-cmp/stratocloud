package com.stratocloud.provider.script;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.guest.command.GuestCommandExecutorFactory;
import com.stratocloud.provider.guest.command.StratoGuestCommandExecutorFactoryRegistry;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.script.RemoteScript;
import com.stratocloud.script.RemoteScriptType;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class RemoteScriptService {
    public RemoteScriptResult execute(Resource guestOsResource,
                                      RemoteScript remoteScript,
                                      Map<String, String> environment){
        RemoteScriptType scriptType = remoteScript.type();

        GuestCommandExecutorFactory<?> commandExecutorFactory = selectFactory(guestOsResource, remoteScript).orElseThrow(
                () -> new StratoException("No script executor found for script type: "+ scriptType)
        );

        RemoteScriptExecutor executor = RemoteScriptExecutorRegistry.getExecutor(
                new RemoteScriptExecutor.ExecutionType(
                        commandExecutorFactory.getCommandType(),
                        scriptType
                )
        ).orElseThrow();

        return executor.execute(commandExecutorFactory, guestOsResource, remoteScript.acceptEnvironment(environment));
    }

    public void validateExecutorExist(Resource guestOsResource,
                                      RemoteScript remoteScript){
        selectFactory(guestOsResource, remoteScript).orElseThrow(
                () -> new StratoException("No script executor found for script type: "+ remoteScript.type())
        );
    }

    private Optional<GuestCommandExecutorFactory<?>> selectFactory(Resource guestOsResource,
                                                                   RemoteScript remoteScript){
        List<GuestCommandExecutorFactory<?>> factories = getCommandExecutorFactories(guestOsResource);
        RemoteScriptType scriptType = remoteScript.type();

        if(Utils.isEmpty(factories))
            return Optional.empty();

        for (GuestCommandExecutorFactory<?> factory : factories) {
            RemoteScriptExecutor.ExecutionType executionType = new RemoteScriptExecutor.ExecutionType(
                    factory.getCommandType(),
                    scriptType
            );

            Optional<RemoteScriptExecutor> executor = RemoteScriptExecutorRegistry.getExecutor(executionType);
            if(executor.isPresent())
                return Optional.of(factory);
        }

        return Optional.empty();
    }



    private List<GuestCommandExecutorFactory<?>> getCommandExecutorFactories(Resource guestOsResource) {
        ResourceHandler resourceHandler = guestOsResource.getResourceHandler();

        if(!(resourceHandler instanceof GuestOsHandler guestOsHandler))
            throw new StratoException("Resource %s is not a guest os resource.".formatted(guestOsResource.getName()));

        List<GuestCommandExecutorFactory<?>> result = new ArrayList<>();

        var providerCommandExecutorFactories = guestOsHandler.getProviderCommandExecutorFactories(guestOsResource);

        if(Utils.isNotEmpty(providerCommandExecutorFactories)){
            result.addAll(providerCommandExecutorFactories);
        }


        var stratoGuestCommandExecutorFactory = StratoGuestCommandExecutorFactoryRegistry.getByOsType(
                guestOsHandler.getOsTypeQuietly(guestOsResource)
        );

        stratoGuestCommandExecutorFactory.ifPresent(result::add);

        return result;
    }


}
