package com.stratocloud.provider.guest;

import com.stratocloud.provider.RuntimePropertiesUtil;
import com.stratocloud.provider.guest.command.ProviderGuestCommandExecutorFactory;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.OsType;
import com.stratocloud.resource.Resource;

import java.util.List;

public interface GuestOsHandler extends ResourceHandler {

    OsType getOsType(Resource resource);

    default OsType getOsTypeQuietly(Resource resource){
        try {
            return getOsType(resource);
        }catch (Exception e){
            return OsType.Unknown;
        }
    }

    default GuestManagementInfo getGuestManagementInfo(Resource resource) {
        return RuntimePropertiesUtil.retrieveGuestManagementInfo(resource);
    }

    default List<ProviderGuestCommandExecutorFactory> getProviderCommandExecutorFactories(Resource resource){
        return List.of();
    }

}
