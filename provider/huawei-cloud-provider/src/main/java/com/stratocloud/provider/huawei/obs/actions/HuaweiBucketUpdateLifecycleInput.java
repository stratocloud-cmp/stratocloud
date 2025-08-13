package com.stratocloud.provider.huawei.obs.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.obs.services.model.BucketTagInfo;
import com.obs.services.model.LifecycleConfiguration;
import com.obs.services.model.StorageClassEnum;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.form.*;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.common.services.HuaweiObsService;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.utils.TimeUtil;
import com.stratocloud.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Data
public class HuaweiBucketUpdateLifecycleInput implements ResourceActionInput {
    @BooleanField(label = "开启生命周期")
    private boolean enabled;

    @NestedFormField(
            label = "生命周期规则",
            multiple = true,
            nestedFormClass = RuleInput.class,
            conditions = "this.enabled === true"
    )
    private List<RuleInput> rules;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Tag implements DynamicForm {
        @InputField(label = "标签键")
        private String key;
        @InputField(label = "标签值")
        private String value;
    }

    public enum TransitionType {
        DAYS,
        DATE,
        DELETE_MARKER,
        DISABLED
    }

    @Data
    public static class Transition implements DynamicForm {
        @SelectField(
                label = "沉降策略",
                options = {
                        "DAYS",
                        "DATE"
                },
                optionNames = {
                        "按天数",
                        "按日期"
                },
                defaultValues = "DAYS"
        )
        private TransitionType type;
        @SelectField(
                label = "沉降至该存储类型",
                options = {
                        "WARM",
                        "COLD",
                        "DEEP_ARCHIVE"
                },
                optionNames = {
                        "低频访问存储",
                        "归档存储",
                        "深度冷归档存储"
                }
        )
        private StorageClassEnum storageClass;
        @NumberField(label = "该天数后沉降", conditions = "this.type === 'DAYS'")
        private int days;
        @DateTimeField(label = "该日期后沉降", conditions = "this.type === 'DATE'", dateOnly = true)
        private LocalDateTime expirationDate;

        @JsonIgnore
        public void validate() {

        }

        public LifecycleConfiguration.Transition toStorageTransition(LifecycleConfiguration configuration) {
            if(type == TransitionType.DATE) {
                return configuration.new Transition(
                        TimeUtil.toDate(expirationDate),
                        storageClass
                );
            } else {
                return configuration.new Transition(
                        days,
                        storageClass
                );
            }
        }

        public static Transition fromStorageTransition(LifecycleConfiguration.Transition storageTransition) {
            Integer expirationDays = storageTransition.getDays();
            Date createdBeforeDate = storageTransition.getDate();

            Transition transition = new Transition();

            if(expirationDays != null && expirationDays > 0) {
                transition.setType(TransitionType.DAYS);
                transition.setDays(expirationDays);
                transition.setStorageClass(storageTransition.getObjectStorageClass());
            } else if(createdBeforeDate != null) {
                transition.setType(TransitionType.DATE);
                transition.setStorageClass(storageTransition.getObjectStorageClass());
                transition.setExpirationDate(TimeUtil.fromDate(createdBeforeDate));
            } else {
                transition.setType(TransitionType.DISABLED);
            }

            return transition;
        }
    }

    @Data
    public static class HistoryTransition implements DynamicForm {
        @SelectField(
                label = "沉降至该存储类型",
                options = {
                        "WARM",
                        "COLD",
                        "DEEP_ARCHIVE"
                },
                optionNames = {
                        "低频访问存储",
                        "归档存储",
                        "深度冷归档存储"
                }
        )
        private StorageClassEnum storageClass;
        @NumberField(label = "该天数后沉降")
        private int days;

        @JsonIgnore
        public void validate() {

        }

        public LifecycleConfiguration.NoncurrentVersionTransition toStorageTransition(LifecycleConfiguration configuration) {
            return configuration.new NoncurrentVersionTransition(
                    days,
                    storageClass
            );
        }

        public static HistoryTransition fromStorageTransition(LifecycleConfiguration.NoncurrentVersionTransition transition) {
            HistoryTransition historyTransition = new HistoryTransition();
            historyTransition.setStorageClass(transition.getObjectStorageClass());
            if(transition.getDays() != null)
                historyTransition.setDays(transition.getDays());

            return historyTransition;
        }
    }

    @Data
    public static class RuleInput implements DynamicForm {
        @BooleanField(label = "启用", defaultValue = true)
        private boolean enabled;

        @BooleanField(label = "匹配前缀", defaultValue = true)
        private boolean enablePrefix;
        @InputField(label = "前缀", conditions = "this.enablePrefix === true")
        private String prefix;

        @BooleanField(label = "匹配标签", conditions = "this.expirationType !== 'DELETE_MARKER'")
        private boolean enableTags;
        @NestedFormField(
                label = "标签",
                multiple = true,
                nestedFormClass = Tag.class,
                conditions = "this.enableTags === true && this.expirationType !== 'DELETE_MARKER'"
        )
        private List<Tag> tags;

        @SelectField(
                label = "当前版本文件删除策略",
                options = {
                        "DAYS",
                        "DATE",
                        "DELETE_MARKER",
                        "DISABLED"
                },
                optionNames = {
                        "按天数",
                        "按日期",
                        "清理过期删除标记",
                        "不启用"
                },
                defaultValues = "DISABLED"
        )
        private TransitionType expirationType;
        @NumberField(label = "该天数后删除当前版本文件", conditions = "this.expirationType === 'DAYS'")
        private int expirationDays;
        @DateTimeField(label = "该日期后删除当前版本文件", conditions = "this.expirationType === 'DATE'", dateOnly = true)
        private LocalDateTime expirationDate;

        @NestedFormField(label = "当前版本文件沉降策略", multiple = true, nestedFormClass = Transition.class)
        private List<Transition> transitions;

        @SelectField(
                label = "历史版本文件删除策略",
                options = {
                        "DAYS",
                        "DISABLED"
                },
                optionNames = {
                        "按天数",
                        "不启用"
                },
                defaultValues = "DISABLED"
        )
        private TransitionType historyExpirationType;
        @NumberField(label = "该天数后删除历史版本文件", conditions = "this.historyExpirationType === 'DAYS'")
        private int historyExpirationDays;

        @NestedFormField(label = "历史版本文件沉降策略", multiple = true, nestedFormClass = HistoryTransition.class)
        private List<HistoryTransition> historyTransitions;


        @SelectField(
                label = "碎片清理策略",
                options = {
                        "DAYS",
                        "DISABLED"
                },
                optionNames = {
                        "按天数",
                        "不启用"
                },
                defaultValues = "DISABLED"
        )
        private TransitionType fragmentExpirationType;
        @NumberField(label = "该天数后清理碎片", conditions = "this.fragmentExpirationType === 'DAYS'")
        private int fragmentExpirationDays;

        @JsonIgnore
        public void validate(){
            TransitionType disabled = TransitionType.DISABLED;

            if(expirationType == disabled && historyExpirationType == disabled && fragmentExpirationType == disabled){
                if(Utils.isEmpty(transitions) && Utils.isEmpty(historyTransitions))
                    throw new BadCommandException("请至少指定一条策略");
            }

            if(Utils.isNotEmpty(transitions))
                transitions.forEach(Transition::validate);

            if(Utils.isNotEmpty(historyTransitions))
                historyTransitions.forEach(HistoryTransition::validate);
        }
    }

    @JsonIgnore
    public void validate(){
        if(Utils.isNotEmpty(rules))
            rules.forEach(RuleInput::validate);
    }


    @JsonIgnore
    public LifecycleConfiguration toLifecycleConfiguration(){
        LifecycleConfiguration configuration = new LifecycleConfiguration();
        List<LifecycleConfiguration.Rule> lifecycleRules = new ArrayList<>();


        if(!enabled || Utils.isEmpty(rules))
            return configuration;

        for (RuleInput ruleInput : rules) {
            LifecycleConfiguration.Rule rule = configuration.new Rule();

            rule.setEnabled(ruleInput.isEnabled());

            if(ruleInput.isEnablePrefix()){
                rule.setPrefix(ruleInput.getPrefix());
            } else {
                rule.setPrefix("");
            }

            boolean deleteMarker = ruleInput.getExpirationType() == TransitionType.DELETE_MARKER;

            if(!deleteMarker && ruleInput.isEnableTags() && Utils.isNotEmpty(ruleInput.getTags())){
                BucketTagInfo.TagSet tagSet = new BucketTagInfo.TagSet();
                ruleInput.getTags().forEach(t -> tagSet.addTag(t.getKey(), t.getValue()));
                rule.setTagSet(tagSet);
            }

            switch (ruleInput.getExpirationType()){
                case DAYS -> rule.newExpiration().setDays(ruleInput.getExpirationDays());
                case DATE -> rule.newExpiration().setDate(TimeUtil.toDate(ruleInput.getExpirationDate()));
                case DELETE_MARKER -> rule.newExpiration().setExpiredObjectDeleteMarker(true);
            }

            if(Utils.isNotEmpty(ruleInput.getTransitions()))
                rule.setTransitions(
                        ruleInput.getTransitions().stream().map(
                                t -> t.toStorageTransition(configuration)
                        ).toList()
                );

            if(ruleInput.getHistoryExpirationType() == TransitionType.DAYS){
                rule.newNoncurrentVersionExpiration().setDays(ruleInput.getHistoryExpirationDays());
            }

            if(Utils.isNotEmpty(ruleInput.getHistoryTransitions()))
                rule.setNoncurrentVersionTransitions(
                        ruleInput.getHistoryTransitions().stream().map(
                                t -> t.toStorageTransition(configuration)
                        ).toList()
                );

            if(ruleInput.getFragmentExpirationType() == TransitionType.DAYS){
                var multipartUpload = configuration.new AbortIncompleteMultipartUpload();
                multipartUpload.setDaysAfterInitiation(ruleInput.getFragmentExpirationDays());
                rule.setAbortIncompleteMultipartUpload(
                        multipartUpload
                );
            }

            lifecycleRules.add(rule);
        }

        configuration.getRules().addAll(lifecycleRules);

        return configuration;
    }

    public static HuaweiBucketUpdateLifecycleInput getInput(HuaweiCloudClient client, String bucketName){
        HuaweiBucketUpdateLifecycleInput input = new HuaweiBucketUpdateLifecycleInput();

        HuaweiObsService obsService = client.obs();
        Optional<LifecycleConfiguration> configuration = obsService.describeBucketLifecycle(bucketName);

        if(configuration.isEmpty())
            return input;

        List<LifecycleConfiguration.Rule> lifecycleRules = configuration.get().getRules();

        if(Utils.isNotEmpty(lifecycleRules)){
            input.setEnabled(true);

            List<RuleInput> ruleInputs = new ArrayList<>();
            for (LifecycleConfiguration.Rule lifecycleRule : lifecycleRules) {
                RuleInput ruleInput = new RuleInput();

                if(lifecycleRule.getEnabled() != null)
                    ruleInput.setEnabled(lifecycleRule.getEnabled());

                if(Utils.isNotBlank(lifecycleRule.getPrefix())){
                    ruleInput.setEnablePrefix(true);
                    ruleInput.setPrefix(lifecycleRule.getPrefix());
                }

                BucketTagInfo.TagSet tagSet = lifecycleRule.getTagSet();
                if(tagSet != null && Utils.isNotEmpty(tagSet.getTags())){
                    ruleInput.setEnableTags(true);
                    ruleInput.setTags(
                            tagSet.getTags().stream().map(
                                    t -> new Tag(t.getKey(), t.getValue())
                            ).toList()
                    );
                }

                LifecycleConfiguration.Expiration expiration = lifecycleRule.getExpiration();

                if(expiration != null){
                    Integer expirationDays = expiration.getDays();
                    Date expirationTime = expiration.getDate();
                    Boolean expiredDeleteMarker = expiration.getExpiredObjectDeleteMarker();

                    if(expirationDays != null && expirationDays > 0){
                        ruleInput.setExpirationType(TransitionType.DAYS);
                        ruleInput.setExpirationDays(expirationDays);
                    } else if(expirationTime != null){
                        ruleInput.setExpirationType(TransitionType.DATE);
                        ruleInput.setExpirationDate(TimeUtil.fromDate(expirationTime));
                    } else if(expiredDeleteMarker != null && expiredDeleteMarker){
                        ruleInput.setExpirationType(TransitionType.DELETE_MARKER);
                    } else {
                        ruleInput.setExpirationType(TransitionType.DISABLED);
                    }
                }else {
                    ruleInput.setExpirationType(TransitionType.DISABLED);
                }



                if(Utils.isNotEmpty(lifecycleRule.getTransitions()))
                    ruleInput.setTransitions(
                            lifecycleRule.getTransitions().stream().map(
                                    Transition::fromStorageTransition
                            ).toList()
                    );

                var noncurrentVersionExpiration = lifecycleRule.getNoncurrentVersionExpiration();

                if(noncurrentVersionExpiration != null && noncurrentVersionExpiration.getDays() != null){
                    ruleInput.setHistoryExpirationType(TransitionType.DAYS);
                    ruleInput.setHistoryExpirationDays(noncurrentVersionExpiration.getDays());
                } else {
                    ruleInput.setHistoryExpirationType(TransitionType.DISABLED);
                }

                if(Utils.isNotEmpty(lifecycleRule.getNoncurrentVersionTransitions()))
                    ruleInput.setHistoryTransitions(
                            lifecycleRule.getNoncurrentVersionTransitions().stream().map(
                                    HistoryTransition::fromStorageTransition
                            ).toList()
                    );

                var abortMultipartUpload = lifecycleRule.getAbortIncompleteMultipartUpload();

                if(abortMultipartUpload != null){
                    if(abortMultipartUpload.getDaysAfterInitiation()>0){
                        ruleInput.setFragmentExpirationType(TransitionType.DAYS);
                        ruleInput.setFragmentExpirationDays(abortMultipartUpload.getDaysAfterInitiation());
                    } else {
                        ruleInput.setFragmentExpirationType(TransitionType.DISABLED);
                    }
                }

                ruleInputs.add(ruleInput);
            }
            input.setRules(ruleInputs);
        } else {
            input.setEnabled(false);
        }

        return input;
    }
}
