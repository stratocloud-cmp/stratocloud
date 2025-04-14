package com.stratocloud.controllers;

import com.stratocloud.exceptions.PremiumOnlyException;
import com.stratocloud.stack.blueprint.BlueprintApi;
import com.stratocloud.stack.blueprint.cmd.CreateBlueprintCmd;
import com.stratocloud.stack.blueprint.cmd.DeleteBlueprintsCmd;
import com.stratocloud.stack.blueprint.cmd.UpdateBlueprintCmd;
import com.stratocloud.stack.blueprint.query.DescribeBlueprintsRequest;
import com.stratocloud.stack.blueprint.query.GenerateCreateStacksCmdRequest;
import com.stratocloud.stack.blueprint.query.GenerateCreateStacksCmdResponse;
import com.stratocloud.stack.blueprint.query.NestedBlueprintResponse;
import com.stratocloud.stack.blueprint.response.CreateBlueprintResponse;
import com.stratocloud.stack.blueprint.response.DeleteBlueprintsResponse;
import com.stratocloud.stack.blueprint.response.UpdateBlueprintResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnMissingClass("com.stratocloud.stack.blueprint.controller.BlueprintController")
public class BlueprintFallbackController implements BlueprintApi {

    private static final String errorMessage = """
            资源栈及蓝图功能未对社区版开放，请邮件咨询商业版: stratocloud@163.com
            """;

    @Override
    public CreateBlueprintResponse createBlueprint(@RequestBody CreateBlueprintCmd cmd) {
        throw new PremiumOnlyException(errorMessage);
    }

    @Override
    public UpdateBlueprintResponse updateBlueprint(@RequestBody UpdateBlueprintCmd cmd) {
        throw new PremiumOnlyException(errorMessage);
    }

    @Override
    public DeleteBlueprintsResponse deleteBlueprints(@RequestBody DeleteBlueprintsCmd cmd) {
        throw new PremiumOnlyException(errorMessage);
    }

    @Override
    public Page<NestedBlueprintResponse> describeBlueprints(@RequestBody DescribeBlueprintsRequest request) {
        throw new PremiumOnlyException(errorMessage);
    }


    @Override
    public GenerateCreateStacksCmdResponse generateCreateStacksCmd(@RequestBody GenerateCreateStacksCmdRequest request) {
        throw new PremiumOnlyException(errorMessage);
    }
}
