package com.stratocloud.stack.blueprint;

import com.stratocloud.constant.StratoServices;
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
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface BlueprintApi {
    @PostMapping(StratoServices.RESOURCE_SERVICE+"/create-blueprint")
    CreateBlueprintResponse createBlueprint(@RequestBody CreateBlueprintCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/update-blueprint")
    UpdateBlueprintResponse updateBlueprint(@RequestBody UpdateBlueprintCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/delete-blueprints")
    DeleteBlueprintsResponse deleteBlueprints(@RequestBody DeleteBlueprintsCmd cmd);


    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-blueprints")
    Page<NestedBlueprintResponse> describeBlueprints(@RequestBody DescribeBlueprintsRequest request);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/generate-create-stacks-cmd")
    GenerateCreateStacksCmdResponse generateCreateStacksCmd(@RequestBody GenerateCreateStacksCmdRequest request);
}
