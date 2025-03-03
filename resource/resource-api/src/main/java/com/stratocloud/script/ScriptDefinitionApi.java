package com.stratocloud.script;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.script.cmd.*;
import com.stratocloud.script.query.DescribeScriptDefinitionsRequest;
import com.stratocloud.script.query.NestedScriptDefinitionResponse;
import com.stratocloud.script.response.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;

public interface ScriptDefinitionApi {
    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-script-definitions")
    Page<NestedScriptDefinitionResponse> describeScriptDefinitions(DescribeScriptDefinitionsRequest request);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/create-script-definition")
    CreateScriptDefinitionResponse createScriptDefinition(CreateScriptDefinitionCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/update-script-definition")
    UpdateScriptDefinitionResponse updateScriptDefinition(UpdateScriptDefinitionCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/delete-script-definitions")
    DeleteScriptDefinitionsResponse deleteScriptDefinitions(DeleteScriptDefinitionsCmd cmd);


    @PostMapping(StratoServices.RESOURCE_SERVICE+"/enable-script-definitions")
    EnableScriptDefinitionsResponse enableScriptDefinitions(EnableScriptDefinitionsCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/disable-script-definitions")
    DisableScriptDefinitionsResponse disableScriptDefinitions(DisableScriptDefinitionsCmd cmd);
}
