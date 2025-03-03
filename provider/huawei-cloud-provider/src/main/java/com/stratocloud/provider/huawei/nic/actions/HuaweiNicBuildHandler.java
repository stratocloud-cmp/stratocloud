package com.stratocloud.provider.huawei.nic.actions;

import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.huawei.nic.HuaweiNicHandler;
import com.stratocloud.provider.huawei.nic.HuaweiNicHelper;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class HuaweiNicBuildHandler implements BuildResourceActionHandler {

    private final HuaweiNicHandler nicHandler;

    private final HuaweiNicHelper nicHelper;


    public HuaweiNicBuildHandler(HuaweiNicHandler nicHandler,
                                 HuaweiNicHelper nicHelper) {
        this.nicHandler = nicHandler;
        this.nicHelper = nicHelper;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return nicHandler;
    }

    @Override
    public String getTaskName() {
        return "创建网卡";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return HuaweiNicBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        if(Utils.isNotBlank(resource.getExternalId())){
            resource.synchronize();
            if(resource.exists()){
                log.warn("Port {} is already created, skipping BUILD action...", resource.getName());
                return;
            }
        }

        nicHelper.createPort(resource, parameters);
    }




    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        HuaweiNicBuildInput input = JSON.convert(parameters, HuaweiNicBuildInput.class);

        if(Utils.isEmpty(input.getIps()))
            return List.of(
                    new ResourceUsage(UsageTypes.NIC_IP.type(), BigDecimal.ONE)
            );
        else
            return List.of(
                    new ResourceUsage(UsageTypes.NIC_IP.type(), BigDecimal.valueOf(input.getIps().size()))
            );
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
