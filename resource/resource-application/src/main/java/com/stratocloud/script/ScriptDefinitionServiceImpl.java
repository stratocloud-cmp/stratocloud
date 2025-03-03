package com.stratocloud.script;

import com.stratocloud.audit.AuditLogContext;
import com.stratocloud.audit.AuditObject;
import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.external.resource.UserGatewayService;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.repository.ScriptDefinitionRepository;
import com.stratocloud.script.cmd.*;
import com.stratocloud.script.query.DescribeScriptDefinitionsRequest;
import com.stratocloud.script.query.NestedScriptDefinitionResponse;
import com.stratocloud.script.response.*;
import com.stratocloud.utils.Utils;
import com.stratocloud.validate.ValidateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ScriptDefinitionServiceImpl implements ScriptDefinitionService {

    private final ScriptDefinitionRepository repository;

    private final UserGatewayService userGatewayService;

    public ScriptDefinitionServiceImpl(ScriptDefinitionRepository repository,
                                       UserGatewayService userGatewayService) {
        this.repository = repository;
        this.userGatewayService = userGatewayService;
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedScriptDefinitionResponse> describeScriptDefinitions(DescribeScriptDefinitionsRequest request) {
        String search = request.getSearch();
        List<Long> tenantIds = request.getTenantIds();
        List<Long> ownerIds = request.getOwnerIds();
        Boolean disabled = request.getDisabled();
        Pageable pageable = request.getPageable();

        Page<ScriptDefinition> page = repository.page(search, tenantIds, ownerIds, disabled, pageable);

        Page<NestedScriptDefinitionResponse> result = page.map(this::toNestedScriptDefinitionResponse);

        EntityUtil.fillOwnerInfo(result, userGatewayService);

        return result;
    }

    private NestedScriptDefinitionResponse toNestedScriptDefinitionResponse(ScriptDefinition scriptDefinition) {
        NestedScriptDefinitionResponse response = new NestedScriptDefinitionResponse();

        EntityUtil.copyBasicFields(scriptDefinition, response);

        response.setDefinitionKey(scriptDefinition.getDefinitionKey());
        response.setName(scriptDefinition.getName());
        response.setDescription(scriptDefinition.getDescription());


        response.setPublicDefinition(scriptDefinition.isPublicDefinition());
        response.setVisibleInTarget(scriptDefinition.isVisibleInTarget());

        response.setDisabled(scriptDefinition.isDisabled());

        response.setRemoteScriptDef(RemoteScriptConverter.toNested(scriptDefinition.getRemoteScriptDef()));

        return response;
    }

    @Override
    @Transactional
    @ValidateRequest
    public CreateScriptDefinitionResponse createScriptDefinition(CreateScriptDefinitionCmd cmd) {
        ScriptDefinition scriptDefinition = newScriptDefinition(cmd);

        scriptDefinition = repository.save(scriptDefinition);

        AuditLogContext.current().addAuditObject(
                new AuditObject(scriptDefinition.getId().toString(), scriptDefinition.getName())
        );

        return new CreateScriptDefinitionResponse(scriptDefinition.getId());
    }

    private ScriptDefinition newScriptDefinition(CreateScriptDefinitionCmd cmd) {
        return new ScriptDefinition(
                cmd.getDefinitionKey(),
                cmd.getName(),
                cmd.getDescription(),
                cmd.isPublicDefinition(),
                cmd.isVisibleInTarget(),
                RemoteScriptConverter.fromNested(cmd.getRemoteScriptDef())
        );
    }

    @Override
    @Transactional
    @ValidateRequest
    public UpdateScriptDefinitionResponse updateScriptDefinition(UpdateScriptDefinitionCmd cmd) {
        Long scriptDefinitionId = cmd.getScriptDefinitionId();
        ScriptDefinition scriptDefinition = findDefinition(scriptDefinitionId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(scriptDefinition.getId().toString(), scriptDefinition.getName())
        );

        scriptDefinition.update(
                cmd.getName(),
                cmd.getDescription(),
                cmd.isPublicDefinition(),
                cmd.isVisibleInTarget(),
                RemoteScriptConverter.fromNested(cmd.getRemoteScriptDef())
        );

        repository.save(scriptDefinition);

        return new UpdateScriptDefinitionResponse();
    }

    private ScriptDefinition findDefinition(Long scriptDefinitionId) {
        return repository.findById(scriptDefinitionId).orElseThrow(
                () -> new EntityNotFoundException("Script definition not found.")
        );
    }

    @Override
    @Transactional
    @ValidateRequest
    public DeleteScriptDefinitionsResponse deleteScriptDefinitions(DeleteScriptDefinitionsCmd cmd) {
        if(Utils.isNotEmpty(cmd.getScriptDefinitionIds()))
            for (Long scriptDefinitionId : cmd.getScriptDefinitionIds())
                deleteScriptDefinition(scriptDefinitionId);

        return new DeleteScriptDefinitionsResponse();
    }

    private void deleteScriptDefinition(Long scriptDefinitionId) {
        ScriptDefinition scriptDefinition = findDefinition(scriptDefinitionId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(scriptDefinition.getId().toString(), scriptDefinition.getName())
        );

        repository.delete(scriptDefinition);
    }


    @Override
    @Transactional
    @ValidateRequest
    public EnableScriptDefinitionsResponse enableScriptDefinitions(EnableScriptDefinitionsCmd cmd) {
        cmd.getScriptDefinitionIds().forEach(this::enableScriptDefinition);
        return new EnableScriptDefinitionsResponse();
    }

    private void enableScriptDefinition(Long scriptDefinitionId) {
        ScriptDefinition definition = findDefinition(scriptDefinitionId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(definition.getId().toString(), definition.getName())
        );

        definition.enable();
        repository.save(definition);
    }


    @Override
    @Transactional
    @ValidateRequest
    public DisableScriptDefinitionsResponse disableScriptDefinitions(DisableScriptDefinitionsCmd cmd) {
        cmd.getScriptDefinitionIds().forEach(this::disableScriptDefinition);
        return new DisableScriptDefinitionsResponse();
    }

    private void disableScriptDefinition(Long scriptDefinitionId) {
        ScriptDefinition definition = findDefinition(scriptDefinitionId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(definition.getId().toString(), definition.getName())
        );

        definition.disable();
        repository.save(definition);
    }
}
