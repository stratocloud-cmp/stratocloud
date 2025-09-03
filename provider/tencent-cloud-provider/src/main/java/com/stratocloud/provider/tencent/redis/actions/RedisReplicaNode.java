package com.stratocloud.provider.tencent.redis.actions;

import com.stratocloud.form.*;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.redis.v20180412.models.RedisNodeInfo;
import lombok.Data;

import java.util.List;

@Data
public class RedisReplicaNode implements DynamicForm {
    @BooleanField(label = "ShowNodeId", conditions = "false")
    private boolean showNodeId;
    @InputField(label = "节点ID", conditions = "this.showNodeId === true", disabled = true)
    private Long nodeId;
    @SelectField(label = "可用区", placeholder = "留空则与主可用区一致", required = false)
    private String zone;

    public static DynamicFormMetaData getFormMetaData(List<String> zones,
                                                      List<String> zoneNames){
        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(RedisReplicaNode.class);

        return DynamicFormHelper.changeOptions(formMetaData, "zone", zones, zoneNames);
    }

    public RedisNodeInfo toNodeInfo(String masterZone){
        RedisNodeInfo nodeInfo = new RedisNodeInfo();
        nodeInfo.setNodeType(1L);
        nodeInfo.setNodeId(nodeId);
        nodeInfo.setZoneName(Utils.isBlank(zone) ? masterZone : zone);
        return nodeInfo;
    }
}
