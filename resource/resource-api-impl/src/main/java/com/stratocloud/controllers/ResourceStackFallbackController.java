package com.stratocloud.controllers;

import com.stratocloud.exceptions.PremiumOnlyException;
import com.stratocloud.stack.runtime.ResourceStackApi;
import com.stratocloud.stack.runtime.cmd.DropResourceStacksCmd;
import com.stratocloud.stack.runtime.cmd.UpdateStackBasicsCmd;
import com.stratocloud.stack.runtime.query.DescribeResourceStacksRequest;
import com.stratocloud.stack.runtime.query.NestedResourceStackResponse;
import com.stratocloud.stack.runtime.response.DropStacksResponse;
import com.stratocloud.stack.runtime.response.UpdateStackBasicsResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnMissingBean(ResourceStackApi.class)
@RestController
public class ResourceStackFallbackController implements ResourceStackApi {

    private static final String errorMessage = """
            资源栈及蓝图功能未对社区版开放，请邮件咨询商业版。
            """;


    @Override
    public Page<NestedResourceStackResponse> describeStacks(@RequestBody DescribeResourceStacksRequest request) {
        throw new PremiumOnlyException(errorMessage);
    }


    @Override
    public UpdateStackBasicsResponse updateStackBasics(@RequestBody UpdateStackBasicsCmd cmd) {
        throw new PremiumOnlyException(errorMessage);
    }

    @Override
    public DropStacksResponse dropStacks(@RequestBody DropResourceStacksCmd cmd) {
        throw new PremiumOnlyException(errorMessage);
    }
}
