package com.stratocloud.resource;

import com.stratocloud.job.JobContext;
import com.stratocloud.repository.ResourceRepository;
import com.stratocloud.resource.query.NestedResourceResponse;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
public class ResourceJobHelper {

    private final ResourceRepository resourceRepository;

    private final ResourceAssembler resourceAssembler;

    public ResourceJobHelper(ResourceRepository resourceRepository,
                             ResourceAssembler resourceAssembler) {
        this.resourceRepository = resourceRepository;
        this.resourceAssembler = resourceAssembler;
    }

    @Transactional(readOnly = true)
    public void addResourcesToJobContext(List<Long> resourceIds) {
        if(!JobContext.exists())
            return;

        if(Utils.isEmpty(resourceIds))
            return;

        try {
            List<NestedResourceResponse> responseList = getNestedResources(resourceIds);
            JobContext.current().addOutput(
                    "resources",
                    responseList
            );
        }catch (Exception e){
            log.warn("Failed to add resources to job context.", e);
        }
    }

    @Transactional(readOnly = true)
    public List<NestedResourceResponse> getNestedResources(List<Long> resourceIds) {
        if(Utils.isEmpty(resourceIds))
            return List.of();

        List<Resource> resources = resourceRepository.findAllById(resourceIds.stream().distinct().toList());
        return resourceAssembler.convertList(resources);
    }
}
