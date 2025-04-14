package com.stratocloud.workflow.nodes;

import com.stratocloud.identity.SimpleUser;
import com.stratocloud.utils.Assert;
import com.stratocloud.workflow.Node;
import com.stratocloud.workflow.NodeProperties;
import com.stratocloud.workflow.factory.ConfirmNodeProperties;
import com.stratocloud.workflow.runtime.ConfirmNodeInstance;
import com.stratocloud.workflow.runtime.NodeInstance;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConfirmNode extends Node {
    @Column
    private List<Long> possibleHandlerIds = new ArrayList<>();
    @Column
    private List<String> possibleHandlerLoginNames = new ArrayList<>();
    @Column
    private List<String> possibleHandlerNames = new ArrayList<>();

    public ConfirmNode(List<SimpleUser> possibleHandlers){
        this.possibleHandlerIds = possibleHandlers.stream().map(SimpleUser::userId).toList();
        this.possibleHandlerLoginNames = possibleHandlers.stream().map(SimpleUser::loginName).toList();
        this.possibleHandlerNames = possibleHandlers.stream().map(SimpleUser::realName).toList();
    }

    public List<SimpleUser> getPossibleHandlers() {
        List<SimpleUser> result = new ArrayList<>();
        for (int i = 0; i < possibleHandlerIds.size(); i++) {
            result.add(
                    new SimpleUser(
                            possibleHandlerIds.get(i),
                            possibleHandlerLoginNames.get(i),
                            possibleHandlerNames.get(i)
                    )
            );
        }
        return result;
    }


    @Override
    public NodeInstance createInstance(Map<String, Object> runtimeProperties) {
        return new ConfirmNodeInstance(this);
    }

    @Override
    public NodeProperties getProperties() {
        ConfirmNodeProperties properties = new ConfirmNodeProperties();
        properties.setConfirmHandlerIds(possibleHandlerIds);
        return properties;
    }

    @Override
    public void validate() {
        super.validate();
        Assert.isNotEmpty(possibleHandlerIds, "节点[%s]未选择处理人".formatted(getName()));
    }

    @Override
    public boolean requireOrder() {
        return true;
    }
}
