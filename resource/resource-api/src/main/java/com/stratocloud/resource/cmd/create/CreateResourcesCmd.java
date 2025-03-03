package com.stratocloud.resource.cmd.create;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stratocloud.request.ApiCommand;
import com.stratocloud.utils.Utils;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Data
public class CreateResourcesCmd implements ApiCommand {
    private Long tenantId;

    private Long ownerId;

    private NestedNewResource resource;

    private List<NestedNewRequirement> requirements;

    private List<NestedNewCapability> capabilities;

    private Integer number = 1;


    @JsonIgnore
    public List<NestedNewResource> getAllResourcesRecursively(){
        List<NestedNewResource> result = new ArrayList<>();
        result.add(resource);

        Queue<NestedNewCapability> queue = new LinkedList<>();
        if(Utils.isNotEmpty(capabilities))
            queue.addAll(capabilities);

        while (!queue.isEmpty()){
            NestedNewCapability capability = queue.poll();
            result.add(capability.getResource());

            if(Utils.isNotEmpty(capability.getCapabilities()))
                capability.getCapabilities().forEach(queue::offer);
        }

        return result;
    }
}
