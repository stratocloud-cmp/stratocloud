package com.stratocloud.script;

import com.stratocloud.audit.AuditLogContext;
import com.stratocloud.audit.AuditObject;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.external.resource.UserGatewayService;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.provider.ProviderRegistry;
import com.stratocloud.repository.ResourceRepository;
import com.stratocloud.repository.SoftwareDefinitionRepository;
import com.stratocloud.resource.ResourceFilters;
import com.stratocloud.script.cmd.*;
import com.stratocloud.script.query.DescribeSoftwareDefinitionsRequest;
import com.stratocloud.script.query.NestedSoftwareDefinitionResponse;
import com.stratocloud.script.response.*;
import com.stratocloud.utils.Utils;
import com.stratocloud.validate.ValidateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SoftwareDefinitionServiceImpl implements SoftwareDefinitionService {

    private final SoftwareDefinitionRepository repository;

    private final ResourceRepository resourceRepository;

    private final UserGatewayService userGatewayService;

    public SoftwareDefinitionServiceImpl(SoftwareDefinitionRepository repository,
                                         ResourceRepository resourceRepository,
                                         UserGatewayService userGatewayService) {
        this.repository = repository;
        this.resourceRepository = resourceRepository;
        this.userGatewayService = userGatewayService;
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedSoftwareDefinitionResponse> describeSoftwareDefinitions(DescribeSoftwareDefinitionsRequest request) {
        String search = request.getSearch();
        List<Long> tenantIds = request.getTenantIds();
        List<Long> ownerIds = request.getOwnerIds();
        Pageable pageable = request.getPageable();
        Boolean disabled = request.getDisabled();

        Page<SoftwareDefinition> page = repository.page(search, tenantIds, ownerIds, disabled, pageable);

        Page<NestedSoftwareDefinitionResponse> result = page.map(this::toNestedSoftwareDefinitionResponse);

        EntityUtil.fillOwnerInfo(result, userGatewayService);

        return result;
    }

    private NestedSoftwareDefinitionResponse toNestedSoftwareDefinitionResponse(SoftwareDefinition softwareDefinition) {
        NestedSoftwareDefinitionResponse response = new NestedSoftwareDefinitionResponse();

        EntityUtil.copyBasicFields(softwareDefinition, response);

        response.setDefinitionKey(softwareDefinition.getDefinitionKey());
        response.setName(softwareDefinition.getName());
        response.setDescription(softwareDefinition.getDescription());
        response.setSoftwareType(softwareDefinition.getSoftwareType());
        response.setOsType(softwareDefinition.getOsType());
        response.setPublicDefinition(softwareDefinition.isPublicDefinition());
        response.setVisibleInTarget(softwareDefinition.isVisibleInTarget());
        response.setDisabled(softwareDefinition.isDisabled());

        response.setServicePort(softwareDefinition.getServicePort());

        if(Utils.isNotEmpty(softwareDefinition.getActions())){
            List<NestedSoftwareAction> actions = softwareDefinition.getActions().stream().map(
                    this::toNestedSoftwareAction
            ).toList();
            response.setActions(actions);
        }

        if(Utils.isNotEmpty(softwareDefinition.getRequirements())){
            List<NestedSoftwareRequirement> requirements = softwareDefinition.getRequirements().stream().map(
                    this::toNestedSoftwareRequirement
            ).toList();
            response.setRequirements(requirements);
        }

        return response;
    }

    private NestedSoftwareRequirement toNestedSoftwareRequirement(SoftwareRequirement requirement) {
        NestedSoftwareRequirement nestedSoftwareRequirement = new NestedSoftwareRequirement();

        nestedSoftwareRequirement.setTargetSoftwareDefinitionId(requirement.getTarget().getId());

        nestedSoftwareRequirement.setRequirementKey(requirement.getRequirementKey());
        nestedSoftwareRequirement.setRequirementName(requirement.getRequirementName());
        nestedSoftwareRequirement.setCapabilityName(requirement.getCapabilityName());
        nestedSoftwareRequirement.setExclusive(requirement.isExclusive());

        nestedSoftwareRequirement.setConnectScriptDef(
                RemoteScriptConverter.toNested(requirement.getConnectScriptDef())
        );
        nestedSoftwareRequirement.setDisconnectScriptDef(
                RemoteScriptConverter.toNested(requirement.getDisconnectScriptDef())
        );
        nestedSoftwareRequirement.setCheckConnectionScriptDef(
                RemoteScriptConverter.toNested(requirement.getCheckConnectionScriptDef())
        );

        return nestedSoftwareRequirement;
    }

    private NestedSoftwareAction toNestedSoftwareAction(SoftwareAction softwareAction) {
        NestedSoftwareAction nestedSoftwareAction = new NestedSoftwareAction();
        nestedSoftwareAction.setActionType(softwareAction.getActionType());
        nestedSoftwareAction.setActionId(softwareAction.getActionId());
        nestedSoftwareAction.setActionName(softwareAction.getActionName());

        nestedSoftwareAction.setRemoteScriptDef(RemoteScriptConverter.toNested(softwareAction.getRemoteScriptDef()));

        return nestedSoftwareAction;
    }

    @Override
    @Transactional
    @ValidateRequest
    public CreateSoftwareDefinitionResponse createSoftwareDefinition(CreateSoftwareDefinitionCmd cmd) {
        if(Utils.isEmpty(cmd.getActions()))
            throw new BadCommandException("Software actions not defined.");

        SoftwareDefinition softwareDefinition = newSoftwareDefinition(cmd);

        if(repository.existsByDefinitionKey(softwareDefinition.getDefinitionKey()))
            throw new BadCommandException("软件标识已存在");

        validateActionsAndRequirements(softwareDefinition);

        softwareDefinition = repository.save(softwareDefinition);

        AuditLogContext.current().addAuditObject(
                new AuditObject(softwareDefinition.getId().toString(), softwareDefinition.getName())
        );

        return new CreateSoftwareDefinitionResponse(softwareDefinition.getId());
    }

    private static void validateActionsAndRequirements(SoftwareDefinition softwareDefinition) {
        validateActions(softwareDefinition);
        validateRequirements(softwareDefinition);
    }

    private static void validateRequirements(SoftwareDefinition softwareDefinition) {
        List<SoftwareRequirement> requirements = softwareDefinition.getRequirements();

        Map<Long, List<SoftwareRequirement>> targetIdMappingRequirements = requirements.stream().collect(
                Collectors.groupingBy(r -> r.getTarget().getId())
        );

        for (var entry : targetIdMappingRequirements.entrySet()) {
            if(Utils.length(entry.getValue()) > 1)
                throw new BadCommandException("该软件定义存在重复的依赖对象");
            if(entry.getKey().equals(softwareDefinition.getId()))
                throw new BadCommandException("软件定义的依赖对象不能是自身");
        }
    }

    private static void validateActions(SoftwareDefinition softwareDefinition) {
        List<SoftwareAction> actions = softwareDefinition.getActions();

        Map<SoftwareActionType, List<SoftwareAction>> actionTypeListMap = actions.stream().collect(
                Collectors.groupingBy(SoftwareAction::getActionType)
        );

        Set<SoftwareActionType> allowMultipleActionsTypes = Set.of(SoftwareActionType.CONFIGURE);

        for (var entry : actionTypeListMap.entrySet()) {
            if(allowMultipleActionsTypes.contains(entry.getKey()))
                continue;

            if(Utils.length(entry.getValue()) > 1)
                throw new BadCommandException("Multiple actions of type %s are not allowed".formatted(entry.getKey()));
        }
    }

    private SoftwareDefinition newSoftwareDefinition(CreateSoftwareDefinitionCmd cmd) {
        SoftwareDefinition softwareDefinition = new SoftwareDefinition(
                cmd.getDefinitionKey(),
                cmd.getName(),
                cmd.getDescription(),
                cmd.getSoftwareType(),
                cmd.getOsType(),
                cmd.isPublicDefinition(),
                cmd.isVisibleInTarget(),
                cmd.getServicePort()
        );

        List<SoftwareAction> actions = cmd.getActions().stream().map(this::newSoftwareAction).toList();
        softwareDefinition.updateActions(actions);

        if(Utils.isNotEmpty(cmd.getRequirements())){
            List<SoftwareRequirement> requirements = cmd.getRequirements().stream().map(
                    this::newSoftwareRequirement
            ).toList();

            softwareDefinition.updateRequirements(requirements);
        }



        return softwareDefinition;
    }

    private SoftwareRequirement newSoftwareRequirement(NestedSoftwareRequirement nestedSoftwareRequirement) {
        SoftwareDefinition target = repository.findById(
                nestedSoftwareRequirement.getTargetSoftwareDefinitionId()
        ).orElseThrow(
                () -> new EntityNotFoundException("Software requirement target not found.")
        );

        return new SoftwareRequirement(
                target,
                nestedSoftwareRequirement.getRequirementKey(),
                nestedSoftwareRequirement.getRequirementName(),
                nestedSoftwareRequirement.getCapabilityName(),
                nestedSoftwareRequirement.isExclusive(),
                RemoteScriptConverter.fromNested(nestedSoftwareRequirement.getConnectScriptDef()),
                RemoteScriptConverter.fromNested(nestedSoftwareRequirement.getDisconnectScriptDef()),
                RemoteScriptConverter.fromNested(nestedSoftwareRequirement.getCheckConnectionScriptDef())
        );
    }

    private SoftwareAction newSoftwareAction(NestedSoftwareAction nestedSoftwareAction) {
        return new SoftwareAction(
                nestedSoftwareAction.getActionType(),
                nestedSoftwareAction.getActionId(),
                nestedSoftwareAction.getActionName(),
                RemoteScriptConverter.fromNested(nestedSoftwareAction.getRemoteScriptDef())
        );
    }

    @Override
    @Transactional
    @ValidateRequest
    public UpdateSoftwareDefinitionResponse updateSoftwareDefinition(UpdateSoftwareDefinitionCmd cmd) {
        Long softwareDefinitionId = cmd.getSoftwareDefinitionId();
        SoftwareDefinition softwareDefinition = findDefinition(softwareDefinitionId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(softwareDefinition.getId().toString(), softwareDefinition.getName())
        );

        softwareDefinition.update(
                cmd.getName(),
                cmd.getDescription(),
                cmd.getSoftwareType(),
                cmd.getOsType(),
                cmd.isPublicDefinition(),
                cmd.isVisibleInTarget(),
                cmd.getServicePort()
        );

        if(Utils.isNotEmpty(cmd.getActions())){
            List<SoftwareAction> actions = cmd.getActions().stream().map(this::newSoftwareAction).toList();
            softwareDefinition.updateActions(actions);
        }

        if(Utils.isNotEmpty(cmd.getRequirements())){
            List<SoftwareRequirement> requirements = cmd.getRequirements().stream().map(
                    this::newSoftwareRequirement
            ).toList();
            softwareDefinition.updateRequirements(requirements);
        }

        validateActionsAndRequirements(softwareDefinition);

        repository.save(softwareDefinition);

        return new UpdateSoftwareDefinitionResponse();
    }

    private SoftwareDefinition findDefinition(Long softwareDefinitionId) {
        return repository.findById(softwareDefinitionId).orElseThrow(
                () -> new EntityNotFoundException("Software definition not found.")
        );
    }

    @Override
    @Transactional
    @ValidateRequest
    public DeleteSoftwareDefinitionsResponse deleteSoftwareDefinitions(DeleteSoftwareDefinitionsCmd cmd) {
        if(Utils.isNotEmpty(cmd.getSoftwareDefinitionIds()))
            cmd.getSoftwareDefinitionIds().forEach(this::deleteSoftwareDefinition);

        return new DeleteSoftwareDefinitionsResponse();
    }


    private void deleteSoftwareDefinition(Long softwareDefinitionId) {
        Optional<SoftwareDefinition> softwareDefinition = repository.findById(softwareDefinitionId);

        if(softwareDefinition.isEmpty())
            return;

        AuditLogContext.current().addAuditObject(
                new AuditObject(softwareDefinition.get().getId().toString(), softwareDefinition.get().getName())
        );

        List<String> softwareResourceTypeIds = ProviderRegistry.getProviders().stream().map(
                provider -> softwareDefinition.get().generateSoftwareResourceTypeId(provider.getId())
        ).toList();

        ResourceFilters filters = ResourceFilters.builder()
                .resourceTypes(softwareResourceTypeIds)
                .build();

        long count = resourceRepository.countByFilters(filters);

        if(count > 0)
            throw new BadCommandException("无法删除使用中的软件定义");

        repository.delete(softwareDefinition.get());
    }

    @Override
    @Transactional
    @ValidateRequest
    public EnableSoftwareDefinitionsResponse enableSoftwareDefinitions(EnableSoftwareDefinitionsCmd cmd) {
        cmd.getSoftwareDefinitionIds().forEach(this::enableSoftwareDefinition);
        return new EnableSoftwareDefinitionsResponse();
    }

    private void enableSoftwareDefinition(Long softwareDefinitionId) {
        SoftwareDefinition definition = findDefinition(softwareDefinitionId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(definition.getId().toString(), definition.getName())
        );

        definition.enable();
        repository.save(definition);
    }

    @Override
    @Transactional
    @ValidateRequest
    public DisableSoftwareDefinitionsResponse disableSoftwareDefinitions(DisableSoftwareDefinitionsCmd cmd) {
        cmd.getSoftwareDefinitionIds().forEach(this::disableSoftwareDefinition);
        return new DisableSoftwareDefinitionsResponse();
    }

    private void disableSoftwareDefinition(Long softwareDefinitionId) {
        SoftwareDefinition definition = findDefinition(softwareDefinitionId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(definition.getId().toString(), definition.getName())
        );

        definition.disable();
        repository.save(definition);
    }
}
