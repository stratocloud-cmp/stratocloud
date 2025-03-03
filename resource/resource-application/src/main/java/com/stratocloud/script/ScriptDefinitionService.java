package com.stratocloud.script;

import com.stratocloud.script.cmd.*;
import com.stratocloud.script.query.DescribeScriptDefinitionsRequest;
import com.stratocloud.script.query.NestedScriptDefinitionResponse;
import com.stratocloud.script.response.*;
import org.springframework.data.domain.Page;

public interface ScriptDefinitionService {
    Page<NestedScriptDefinitionResponse> describeScriptDefinitions(DescribeScriptDefinitionsRequest request);

    CreateScriptDefinitionResponse createScriptDefinition(CreateScriptDefinitionCmd cmd);

    UpdateScriptDefinitionResponse updateScriptDefinition(UpdateScriptDefinitionCmd cmd);

    DeleteScriptDefinitionsResponse deleteScriptDefinitions(DeleteScriptDefinitionsCmd cmd);

    EnableScriptDefinitionsResponse enableScriptDefinitions(EnableScriptDefinitionsCmd cmd);

    DisableScriptDefinitionsResponse disableScriptDefinitions(DisableScriptDefinitionsCmd cmd);
}
