package com.stratocloud.provider.tencent.instance;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.resource.RelationshipActionResult;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cvm.v20170312.models.DataDisk;
import com.tencentcloudapi.cvm.v20170312.models.Instance;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TencentInstanceUtil {

    public static RelationshipActionResult checkLastOperationStateForRelationship(TencentInstanceHandler instanceHandler,
                                                                                  ExternalAccount account,
                                                                                  Resource instanceResource) {
        Optional<Instance> instance = instanceHandler.describeInstance(
                account, instanceResource.getExternalId()
        );

        if(instance.isEmpty())
            return RelationshipActionResult.failed("Instance not found.");

        if(Objects.equals("SUCCESS", instance.get().getLatestOperationState()))
            return RelationshipActionResult.finished();

        if(Objects.equals("OPERATING", instance.get().getLatestOperationState()))
            return RelationshipActionResult.inProgress();

        return RelationshipActionResult.failed(instance.get().getLatestOperationErrorMsg());
    }

    public static ResourceActionResult checkLastOperationStateForAction(TencentInstanceHandler instanceHandler,
                                                                        ExternalAccount account,
                                                                        Resource instanceResource) {
        Optional<Instance> instance = instanceHandler.describeInstance(
                account, instanceResource.getExternalId()
        );

        if(instance.isEmpty())
            return ResourceActionResult.failed("Instance not found.");

        if(Objects.equals("SUCCESS", instance.get().getLatestOperationState()))
            return ResourceActionResult.finished();

        if(Objects.equals("OPERATING", instance.get().getLatestOperationState()))
            return ResourceActionResult.inProgress();

        return ResourceActionResult.failed(instance.get().getLatestOperationErrorMsg());
    }

    public static List<String> getInstanceDiskIds(Instance instance){
        List<String> diskIds = new ArrayList<>();

        if(instance.getSystemDisk() != null)
            diskIds.add(instance.getSystemDisk().getDiskId());

        if(Utils.isNotEmpty(instance.getDataDisks())){
            for (DataDisk dataDisk : instance.getDataDisks()) {
                diskIds.add(dataDisk.getDiskId());
            }
        }

        return diskIds;
    }
}
