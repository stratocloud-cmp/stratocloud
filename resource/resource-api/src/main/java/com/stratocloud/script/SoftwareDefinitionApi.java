package com.stratocloud.script;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.script.cmd.*;
import com.stratocloud.script.query.DescribeSoftwareDefinitionsRequest;
import com.stratocloud.script.query.NestedSoftwareDefinitionResponse;
import com.stratocloud.script.response.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;

public interface SoftwareDefinitionApi {
    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-software-definitions")
    Page<NestedSoftwareDefinitionResponse> describeSoftwareDefinitions(DescribeSoftwareDefinitionsRequest request);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/create-software-definition")
    CreateSoftwareDefinitionResponse createSoftwareDefinition(CreateSoftwareDefinitionCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/update-software-definition")
    UpdateSoftwareDefinitionResponse updateSoftwareDefinition(UpdateSoftwareDefinitionCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/delete-software-definitions")
    DeleteSoftwareDefinitionsResponse deleteSoftwareDefinitions(DeleteSoftwareDefinitionsCmd cmd);


    @PostMapping(StratoServices.RESOURCE_SERVICE+"/enable-software-definitions")
    EnableSoftwareDefinitionsResponse enableSoftwareDefinitions(EnableSoftwareDefinitionsCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/disable-software-definitions")
    DisableSoftwareDefinitionsResponse disableSoftwareDefinitions(DisableSoftwareDefinitionsCmd cmd);
}
