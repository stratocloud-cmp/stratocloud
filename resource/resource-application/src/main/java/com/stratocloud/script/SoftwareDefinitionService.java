package com.stratocloud.script;

import com.stratocloud.script.cmd.*;
import com.stratocloud.script.query.DescribeSoftwareDefinitionsRequest;
import com.stratocloud.script.query.NestedSoftwareDefinitionResponse;
import com.stratocloud.script.response.*;
import org.springframework.data.domain.Page;

public interface SoftwareDefinitionService {
    Page<NestedSoftwareDefinitionResponse> describeSoftwareDefinitions(DescribeSoftwareDefinitionsRequest request);

    CreateSoftwareDefinitionResponse createSoftwareDefinition(CreateSoftwareDefinitionCmd cmd);

    UpdateSoftwareDefinitionResponse updateSoftwareDefinition(UpdateSoftwareDefinitionCmd cmd);

    DeleteSoftwareDefinitionsResponse deleteSoftwareDefinitions(DeleteSoftwareDefinitionsCmd cmd);

    EnableSoftwareDefinitionsResponse enableSoftwareDefinitions(EnableSoftwareDefinitionsCmd cmd);

    DisableSoftwareDefinitionsResponse disableSoftwareDefinitions(DisableSoftwareDefinitionsCmd cmd);
}
