package com.stratocloud.provider.huawei.nic.actions;

import com.stratocloud.job.TaskState;
import com.stratocloud.provider.huawei.nic.HuaweiNicHandler;
import com.stratocloud.provider.huawei.nic.HuaweiNicHelper;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HuaweiNicDestroyHandler implements DestroyResourceActionHandler {

    private final HuaweiNicHandler nicHandler;

    private final HuaweiNicHelper nicHelper;


    public HuaweiNicDestroyHandler(HuaweiNicHandler nicHandler,
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
        return "删除网卡";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        nicHelper.deletePort(resource);
    }


    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ResourceActionResult result = DestroyResourceActionHandler.super.checkActionResult(resource, parameters);

        if(result.taskState() != TaskState.FAILED)
            return result;

        nicHelper.ensurePortDeleted(resource);
        resource.onDestroyed();
        return ResourceActionResult.finished();
    }
}
