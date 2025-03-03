package com.stratocloud.stack.runtime;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.stack.runtime.cmd.DropResourceStacksCmd;
import com.stratocloud.stack.runtime.cmd.UpdateStackBasicsCmd;
import com.stratocloud.stack.runtime.query.DescribeResourceStacksRequest;
import com.stratocloud.stack.runtime.query.NestedResourceStackResponse;
import com.stratocloud.stack.runtime.response.DropStacksResponse;
import com.stratocloud.stack.runtime.response.UpdateStackBasicsResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface ResourceStackApi {
    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-resource-stacks")
    Page<NestedResourceStackResponse> describeStacks(@RequestBody DescribeResourceStacksRequest request);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/update-stack-basics")
    UpdateStackBasicsResponse updateStackBasics(@RequestBody UpdateStackBasicsCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/drop-resource-stacks")
    DropStacksResponse dropStacks(@RequestBody DropResourceStacksCmd cmd);

}
