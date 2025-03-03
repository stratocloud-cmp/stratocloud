package com.stratocloud.resource.cmd;

import com.stratocloud.request.BatchJobParameters;
import com.stratocloud.resource.cmd.create.CreateResourcesCmd;
import com.stratocloud.utils.Assert;
import com.stratocloud.utils.Utils;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BatchCreateResourcesCmd implements BatchJobParameters {
    private List<CreateResourcesCmd> resources;

    @Override
    public void validate() {
        Assert.isNotEmpty(resources, "资源参数列表不能为空");
        resources.forEach(CreateResourcesCmd::validate);
    }

    @Override
    public void merge(BatchJobParameters other) {
        if(!(other instanceof BatchCreateResourcesCmd otherCmd))
            return;

        if(Utils.isEmpty(otherCmd.getResources()))
            return;

        if(resources == null)
            resources = new ArrayList<>();

        resources.addAll(otherCmd.getResources());
    }
}
