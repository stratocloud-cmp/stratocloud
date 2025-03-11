package com.stratocloud.rule;

import com.stratocloud.audit.AuditLogContext;
import com.stratocloud.audit.AuditObject;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.jpa.repository.EntityManager;
import com.stratocloud.repository.NamingSequenceRepository;
import com.stratocloud.repository.RuleRepository;
import com.stratocloud.rule.cmd.*;
import com.stratocloud.rule.query.DescribeRulesRequest;
import com.stratocloud.rule.query.NestedRuleResponse;
import com.stratocloud.rule.response.*;
import com.stratocloud.tenant.Tenant;
import com.stratocloud.user.User;
import com.stratocloud.utils.RandomUtil;
import com.stratocloud.utils.Utils;
import com.stratocloud.validate.ValidateRequest;
import org.apache.commons.collections.MapUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RuleServiceImpl implements RuleService{

    private final RuleRepository repository;

    private final NamingSequenceRepository namingSequenceRepository;

    private final EntityManager entityManager;

    public RuleServiceImpl(RuleRepository repository,
                           NamingSequenceRepository namingSequenceRepository,
                           EntityManager entityManager) {
        this.repository = repository;
        this.namingSequenceRepository = namingSequenceRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public ExecuteRuleResponse executeRule(ExecuteRuleCmd cmd) {
        String ruleType = cmd.getRuleType();
        Map<String, Object> parameters = cmd.getParameters();

        Rule rule = repository.findRuleByType(ruleType).orElseThrow(
                ()->new EntityNotFoundException("Rule not found by type %s".formatted(ruleType))
        );

        ensureTenantAndUser(parameters);

        Object output = rule.execute(parameters);

        return new ExecuteRuleResponse(output);
    }

    private void ensureTenantAndUser(Map<String, Object> parameters) {
        if(Utils.isEmpty(parameters))
            return;

        if(parameters.containsKey("tenantId")){
            Tenant tenant = entityManager.findById(Tenant.class, MapUtils.getLong(parameters, "tenantId"));
            parameters.put("tenant", tenant);
        }

        if(parameters.containsKey("ownerId")){
            User owner = entityManager.findById(User.class, MapUtils.getLong(parameters, "ownerId"));
            parameters.put("owner", owner);
        }

        if(parameters.containsKey("userId")){
            User user = entityManager.findById(User.class, MapUtils.getLong(parameters, "userId"));
            parameters.put("user", user);
        }
    }

    @Override
    @Transactional
    @ValidateRequest
    public ExecuteNamingRuleResponse executeNamingRule(ExecuteRuleCmd cmd) {
        String ruleType = cmd.getRuleType();
        Map<String, Object> parameters = cmd.getParameters();

        ensureTenantAndUser(parameters);

        NamingRule namingRule = findNamingRule(ruleType);

        String prefix = namingRule.getPrefix(parameters);

        String suffix = "";

        switch (namingRule.getSuffixType()){
            case NONE -> {}
            case RANDOM_STRING -> suffix = RandomUtil.generateRandomString(namingRule.getSuffixLength());
            case NUMERIC_SEQUENCE -> {
                NamingSequence namingSequence = ensureNamingSequence(namingRule, "STATIC_SEQUENCE");
                suffix = namingSequence.next(namingRule.getSuffixLength());
                namingSequenceRepository.saveWithSystemSession(namingSequence);
            }
            case DYNAMIC_NUMERIC_SEQUENCE -> {
                NamingSequence namingSequence = ensureNamingSequence(namingRule, prefix);
                suffix = namingSequence.next(namingRule.getSuffixLength());
                namingSequenceRepository.saveWithSystemSession(namingSequence);
            }
        }

        return new ExecuteNamingRuleResponse(prefix+suffix);
    }

    private NamingSequence ensureNamingSequence(NamingRule rule, String prefix){
        Optional<NamingSequence> namingSequence = namingSequenceRepository.findNamingSequence(rule.getType(), prefix);

        return namingSequence.orElse(new NamingSequence(rule.getType(), prefix, rule.getSuffixStartNumber()));
    }

    private NamingRule findNamingRule(String ruleType) {
        Rule rule = repository.findRuleByType(ruleType).orElseThrow(
                ()->new EntityNotFoundException("Naming rule not found by type %s".formatted(ruleType))
        );
        return toNamingRule(rule);
    }

    private static NamingRule toNamingRule(Rule rule) {
        if(!(rule instanceof NamingRule namingRule))
            throw new BadCommandException("规则%s非命名规则".formatted(rule.getName()));
        return namingRule;
    }


    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedRuleResponse> describeRules(DescribeRulesRequest request) {
        List<Long> tenantIds = request.getTenantIds();
        List<Long> ruleIds = request.getRuleIds();
        List<String> ruleTypes = request.getRuleTypes();
        String search = request.getSearch();
        Pageable pageable = request.getPageable();

        Page<Rule> rules = repository.page(tenantIds, ruleIds, ruleTypes, search, pageable);

        return rules.map(this::toNestedRuleResponse);
    }

    private NestedRuleResponse toNestedRuleResponse(Rule rule) {
        NestedRuleResponse response = new NestedRuleResponse();

        EntityUtil.copyBasicFields(rule, response);

        response.setType(rule.getType());
        response.setName(rule.getName());
        response.setScript(rule.getScript());

        if(rule instanceof NamingRule namingRule){
            response.setIsNamingRule(true);
            response.setSuffixType(namingRule.getSuffixType());
            response.setSuffixLength(namingRule.getSuffixLength());
            response.setSuffixStartNumber(namingRule.getSuffixStartNumber());
        }else {
            response.setIsNamingRule(false);
        }

        return response;
    }

    @Override
    @Transactional
    @ValidateRequest
    public UpdateRuleResponse updateNamingRule(UpdateNamingRuleCmd cmd) {
        Long ruleId = cmd.getRuleId();
        String name = cmd.getName();
        String script = cmd.getScript();
        SuffixType suffixType = cmd.getSuffixType();
        Integer suffixLength = cmd.getSuffixLength();
        Integer suffixStartNumber = cmd.getSuffixStartNumber();

        Rule rule = repository.findRule(ruleId);
        NamingRule namingRule = toNamingRule(rule);
        SuffixPolicy suffixPolicy = new SuffixPolicy(suffixType, suffixLength, suffixStartNumber);

        namingRule.update(name, script);
        namingRule.updateSuffixPolicy(suffixPolicy);

        repository.save(namingRule);

        return new UpdateRuleResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public UpdateRuleResponse updateRule(UpdateRuleCmd cmd) {
        Long ruleId = cmd.getRuleId();
        String name = cmd.getName();
        String script = cmd.getScript();

        Rule rule = repository.findRule(ruleId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(rule.getId().toString(), rule.getName())
        );

        rule.update(name, script);

        repository.save(rule);

        return new UpdateRuleResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public CreateRuleResponse createRule(CreateRuleCmd cmd) {
        Rule rule = new Rule();

        rule.setType(cmd.getType());
        rule.setName(cmd.getName());
        rule.setScript(cmd.getScript());
        rule.setTenantId(cmd.getTenantId());

        rule = repository.save(rule);

        AuditLogContext.current().addAuditObject(
                new AuditObject(rule.getId().toString(), rule.getName())
        );

        return new CreateRuleResponse(rule.getId());
    }

    @Override
    @Transactional
    @ValidateRequest
    public CreateRuleResponse createNamingRule(CreateNamingRuleCmd cmd) {
        NamingRule rule = new NamingRule();

        rule.setType(cmd.getType());
        rule.setName(cmd.getName());
        rule.setScript(cmd.getScript());
        rule.setTenantId(cmd.getTenantId());

        rule.setSuffixType(cmd.getSuffixType());

        rule.setSuffixLength(cmd.getSuffixLength());
        rule.setSuffixStartNumber(cmd.getSuffixStartNumber());

        rule = (NamingRule) repository.save(rule);

        AuditLogContext.current().addAuditObject(
                new AuditObject(rule.getId().toString(), rule.getName())
        );

        return new CreateRuleResponse(rule.getId());
    }


    @Override
    @Transactional
    @ValidateRequest
    public DeleteRulesResponse deleteRules(DeleteRulesCmd cmd) {
        List<Long> ruleIds = cmd.getRuleIds();

        ruleIds.forEach(this::deleteRule);

        return new DeleteRulesResponse();
    }

    private void deleteRule(Long ruleId) {
        Rule rule = repository.findRule(ruleId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(rule.getId().toString(), rule.getName())
        );

        if(rule.getBuiltIn())
            throw new BadCommandException("内置规则不能删除");

        repository.delete(rule);
    }
}
