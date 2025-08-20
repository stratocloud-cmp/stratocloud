package com.stratocloud.provider.tencent.database.cdb.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stratocloud.form.*;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cdb.v20170320.models.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
public class CdbClusterTopology implements DynamicForm {

    @NestedFormField(
            label = "只读节点",
            nestedFormClass = ReadOnlyNode.class,
            multiple = true,
            multipleMin = 1,
            multipleMax = 5
    )
    private List<ReadOnlyNode> readOnlyNodes;

    public static CdbClusterTopology fromCdb(InstanceInfo instanceInfo) {
        CdbClusterTopology topology = new CdbClusterTopology();

        List<ReadOnlyNode> list = new ArrayList<>();

        if(instanceInfo.getClusterInfo() != null){
            for (ClusterInfo clusterInfo : instanceInfo.getClusterInfo()) {
                if(Objects.equals(clusterInfo.getRole(), "ro")){
                    ReadOnlyNode readOnlyNode = new ReadOnlyNode();
                    readOnlyNode.setZone(clusterInfo.getZone());
                    list.add(readOnlyNode);
                }
            }
        }

        topology.setReadOnlyNodes(list);
        return topology;
    }

    @JsonIgnore
    public ClusterTopology toClusterTopology(String masterZone) {
        ClusterTopology topology = new ClusterTopology();

        ReadWriteNode readWriteNode = new ReadWriteNode();
        readWriteNode.setZone(masterZone);
        topology.setReadWriteNode(readWriteNode);

        if(Utils.isNotEmpty(readOnlyNodes))
            topology.setReadOnlyNodes(
                    readOnlyNodes.stream().map(ReadOnlyNode::convert).toList().toArray(ReadonlyNode[]::new)
            );

        return topology;
    }

    @Data
    public static class ReadOnlyNode implements DynamicForm {
        @SelectField(
                label = "可用区",
                placeholder = "自动分配",
                required = false
        )
        private String zone;

        @JsonIgnore
        public ReadonlyNode convert() {
            ReadonlyNode readonlyNode = new ReadonlyNode();

            if(Utils.isBlank(zone))
                readonlyNode.setIsRandomZone("YES");
            else
                readonlyNode.setZone(zone);

            return readonlyNode;
        }
    }


    public static DynamicFormMetaData getFormMetaData(List<String> zoneOptions,
                                                      List<String> zoneOptionNames) {
        DynamicFormMetaData clusterMetaData = DynamicFormHelper.generateMetaData(CdbClusterTopology.class);
        DynamicFormMetaData readOnlyNodeMetaData = DynamicFormHelper.generateMetaData(ReadOnlyNode.class);

        readOnlyNodeMetaData = DynamicFormHelper.changeOptions(
                readOnlyNodeMetaData, "zone", zoneOptions, zoneOptionNames
        );
        clusterMetaData = DynamicFormHelper.changeNestedFormFieldMetaData(
                clusterMetaData, "readOnlyNodes", readOnlyNodeMetaData
        );

        return clusterMetaData;
    }
}
