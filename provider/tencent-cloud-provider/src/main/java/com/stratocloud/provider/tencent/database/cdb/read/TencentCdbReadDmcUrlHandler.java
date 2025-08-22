package com.stratocloud.provider.tencent.database.cdb.read;

import com.stratocloud.provider.constants.DbActions;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.provider.tencent.database.cdb.TencentCdbHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceAction;
import com.stratocloud.resource.ResourceReadActionResult;
import com.stratocloud.resource.ResourceState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class TencentCdbReadDmcUrlHandler implements ResourceReadActionHandler {

    private final TencentCdbHandler cdbHandler;

    public TencentCdbReadDmcUrlHandler(TencentCdbHandler cdbHandler) {
        this.cdbHandler = cdbHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return cdbHandler;
    }

    @Override
    public ResourceAction getAction() {
        return DbActions.OPEN_DMC;
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return ResourceState.getAliveStateSet();
    }

    @Override
    public List<ResourceReadActionResult> performReadAction(Resource resource) {
        return List.of(
                new ResourceReadActionResult(
                        "DMC控制台",
                        "https://dms.cloud.tencent.com/datasource",
                        true
                )
        );
    }
}
