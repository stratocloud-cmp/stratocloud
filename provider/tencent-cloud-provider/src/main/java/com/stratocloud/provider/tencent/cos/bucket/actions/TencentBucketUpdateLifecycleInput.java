package com.stratocloud.provider.tencent.cos.bucket.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qcloud.cos.model.AbortIncompleteMultipartUpload;
import com.qcloud.cos.model.BucketLifecycleConfiguration;
import com.qcloud.cos.model.Tag.LifecycleTagPredicate;
import com.qcloud.cos.model.lifecycle.LifecycleAndOperator;
import com.qcloud.cos.model.lifecycle.LifecycleFilter;
import com.qcloud.cos.model.lifecycle.LifecycleFilterPredicate;
import com.qcloud.cos.model.lifecycle.LifecyclePrefixPredicate;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.form.*;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.tencent.cos.session.CosSession;
import com.stratocloud.utils.TimeUtil;
import com.stratocloud.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class TencentBucketUpdateLifecycleInput implements ResourceActionInput {
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

    public enum StorageClass {
        STANDARD_IA("STANDARD_IA", "MAZ_STANDARD_IA"),
        ARCHIVE("ARCHIVE", "MAZ_ARCHIVE"),
        DEEP_ARCHIVE("DEEP_ARCHIVE", "DEEP_ARCHIVE"),
        UNKNOWN("UNKNOWN", "UNKNOWN");

        private final String singleAzName;
        private final String multiAzName;

        StorageClass(String singleAzName, String multiAzName) {
            this.singleAzName = singleAzName;
            this.multiAzName = multiAzName;
        }

        public String getName(boolean isMultiAz){
            return isMultiAz ? multiAzName : singleAzName;
        }

        public static StorageClass parse(String s){
            for (StorageClass storageClass : values()) {
                if(storageClass.singleAzName.equals(s) || storageClass.multiAzName.equals(s))
                    return storageClass;
            }

            return UNKNOWN;
        }
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
                        "STANDARD_IA",
                        "ARCHIVE",
                        "DEEP_ARCHIVE"
                },
                optionNames = {
                        "低频存储",
                        "归档存储",
                        "深度归档存储"
                }
        )
        private StorageClass storageClass;
        @NumberField(label = "该天数后沉降", conditions = "this.type === 'DAYS'")
        private int days;
        @DateTimeField(label = "该日期后沉降", conditions = "this.type === 'DATE'", dateOnly = true)
        private LocalDateTime expirationDate;

        @JsonIgnore
        public void validate() {

        }

        public BucketLifecycleConfiguration.Transition toStorageTransition(boolean isMultiAz) {
            if(type == TransitionType.DATE)
                return new BucketLifecycleConfiguration.Transition().withDate(
                        TimeUtil.toDate(expirationDate)
                ).withStorageClass(
                        storageClass.getName(isMultiAz)
                );
            else
                return new BucketLifecycleConfiguration.Transition().withDays(
                        days
                ).withStorageClass(
                        storageClass.getName(isMultiAz)
                );
        }

        public static Transition fromStorageTransition(BucketLifecycleConfiguration.Transition storageTransition) {
            int expirationDays = storageTransition.getDays();
            Date createdBeforeDate = storageTransition.getDate();

            Transition transition = new Transition();

            if(expirationDays > 0) {
                transition.setType(TransitionType.DAYS);
                transition.setStorageClass(StorageClass.parse(storageTransition.getStorageClass()));
                transition.setDays(expirationDays);
            } else if(createdBeforeDate != null) {
                transition.setType(TransitionType.DATE);
                transition.setStorageClass(StorageClass.parse(storageTransition.getStorageClass()));
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
                        "STANDARD_IA",
                        "ARCHIVE",
                        "DEEP_ARCHIVE"
                },
                optionNames = {
                        "低频存储",
                        "归档存储",
                        "深度归档存储"
                }
        )
        private StorageClass storageClass;
        @NumberField(label = "该天数后沉降")
        private int days;

        @JsonIgnore
        public void validate() {

        }

        public BucketLifecycleConfiguration.NoncurrentVersionTransition toStorageTransition(boolean isMultiAz) {
            return new BucketLifecycleConfiguration.NoncurrentVersionTransition().withDays(
                    days
            ).withStorageClass(
                    storageClass.getName(isMultiAz)
            );
        }

        public static HistoryTransition fromStorageTransition(BucketLifecycleConfiguration.NoncurrentVersionTransition transition) {
            HistoryTransition historyTransition = new HistoryTransition();
            historyTransition.setStorageClass(StorageClass.parse(transition.getStorageClassAsString()));
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

        @BooleanField(label = "匹配标签")
        private boolean enableTags;
        @NestedFormField(
                label = "标签",
                multiple = true,
                nestedFormClass = Tag.class,
                conditions = "this.enableTags === true"
        )
        private List<Tag> tags;

        @NumberField(
                label = "最小文件大小(B)",
                required = false,
                placeHolder = "留空代表不限制"
        )
        private Long objectSizeGreaterThan;
        @NumberField(
                label = "最大文件大小(B)",
                required = false,
                placeHolder = "留空代表不限制"
        )
        private Long objectSizeLessThan;

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
        @DateTimeField(label = "该日期后清理碎片", conditions = "this.fragmentExpirationType === 'DATE'", dateOnly = true)
        private LocalDateTime fragmentExpirationDate;

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
    public BucketLifecycleConfiguration toConfig(boolean isMultiAz){
        List<BucketLifecycleConfiguration.Rule> result = new ArrayList<>();
        BucketLifecycleConfiguration config = new BucketLifecycleConfiguration();
        config.setRules(result);

        if(!enabled || Utils.isEmpty(rules))
            return config;

        for (RuleInput ruleInput : rules) {
            var rule = new BucketLifecycleConfiguration.Rule();

            rule.setStatus(
                    ruleInput.isEnabled() ? BucketLifecycleConfiguration.ENABLED : BucketLifecycleConfiguration.DISABLED
            );

            List<LifecycleFilterPredicate> predicates = new ArrayList<>();
            if(ruleInput.isEnablePrefix()){
                predicates.add(
                        new LifecyclePrefixPredicate(ruleInput.getPrefix())
                );
            }

            if(ruleInput.isEnableTags() && Utils.isNotEmpty(ruleInput.getTags())){
                ruleInput.getTags().forEach(
                        t -> predicates.add(
                                new LifecycleTagPredicate(
                                        new com.qcloud.cos.model.Tag.Tag(t.getKey(), t.getValue())
                                )
                        )
                );
            }

            if(!predicates.isEmpty()){
                LifecycleFilter filter = rule.getFilter();

                if(filter == null){
                    filter = new LifecycleFilter();
                    rule.setFilter(filter);
                }

                if(predicates.size() == 1)
                    filter.setPredicate(predicates.get(0));
                else
                    filter.setPredicate(
                            new LifecycleAndOperator(predicates)
                    );
            }

            switch (ruleInput.getExpirationType()){
                case DAYS -> rule.setExpirationInDays(ruleInput.getExpirationDays());
                case DATE -> rule.setExpirationDate(TimeUtil.toDate(ruleInput.getExpirationDate()));
                case DELETE_MARKER -> rule.setExpiredObjectDeleteMarker(true);
            }

            if(Utils.isNotEmpty(ruleInput.getTransitions()))
                rule.setTransitions(
                        ruleInput.getTransitions().stream().map(
                                t -> t.toStorageTransition(isMultiAz)
                        ).toList()
                );

            if(ruleInput.getHistoryExpirationType() == TransitionType.DAYS)
                rule.setNoncurrentVersionExpirationInDays(ruleInput.getHistoryExpirationDays());


            if(Utils.isNotEmpty(ruleInput.getHistoryTransitions()))
                rule.setNoncurrentVersionTransitions(
                        ruleInput.getHistoryTransitions().stream().map(
                                t -> t.toStorageTransition(isMultiAz)
                        ).toList()
                );

            if(ruleInput.getFragmentExpirationType() == TransitionType.DAYS){
                rule.setAbortIncompleteMultipartUpload(
                        new AbortIncompleteMultipartUpload().withDaysAfterInitiation(
                                ruleInput.getFragmentExpirationDays()
                        )
                );
            }

            result.add(rule);
        }

        return config;
    }

    public static TencentBucketUpdateLifecycleInput getInput(CosSession cosSession, String bucketName){
        TencentBucketUpdateLifecycleInput input = new TencentBucketUpdateLifecycleInput();

        var config = cosSession.describeBucketLifecycle(bucketName);



        if(config.isPresent() && Utils.isNotEmpty(config.get().getRules())){
            input.setEnabled(true);

            List<RuleInput> ruleInputs = new ArrayList<>();
            for (var lifecycleRule : config.get().getRules()) {
                RuleInput ruleInput = new RuleInput();

                ruleInput.setEnabled(
                        BucketLifecycleConfiguration.ENABLED.equals(lifecycleRule.getStatus())
                );

                LifecycleFilter filter = lifecycleRule.getFilter();

                if(filter != null && filter.getPredicate() != null)
                    resolvePredicate(filter.getPredicate(), ruleInput);

                int expirationDays = lifecycleRule.getExpirationInDays();
                Date expirationTime = lifecycleRule.getExpirationDate();
                boolean expiredDeleteMarker = lifecycleRule.isExpiredObjectDeleteMarker();

                if(expirationDays > 0){
                    ruleInput.setExpirationType(TransitionType.DAYS);
                    ruleInput.setExpirationDays(expirationDays);
                } else if(expirationTime != null){
                    ruleInput.setExpirationType(TransitionType.DATE);
                    ruleInput.setExpirationDate(TimeUtil.fromDate(expirationTime));
                } else if(expiredDeleteMarker){
                    ruleInput.setExpirationType(TransitionType.DELETE_MARKER);
                } else {
                    ruleInput.setExpirationType(TransitionType.DISABLED);
                }

                if(Utils.isNotEmpty(lifecycleRule.getTransitions()))
                    ruleInput.setTransitions(
                            lifecycleRule.getTransitions().stream().map(
                                    Transition::fromStorageTransition
                            ).toList()
                    );

                int noncurrentVersionExpiration = lifecycleRule.getNoncurrentVersionExpirationInDays();

                if(noncurrentVersionExpiration > 0){
                    ruleInput.setHistoryExpirationType(TransitionType.DAYS);
                    ruleInput.setHistoryExpirationDays(noncurrentVersionExpiration);
                } else {
                    ruleInput.setHistoryExpirationType(TransitionType.DISABLED);
                }

                if(Utils.isNotEmpty(lifecycleRule.getNoncurrentVersionTransitions()))
                    ruleInput.setHistoryTransitions(
                            lifecycleRule.getNoncurrentVersionTransitions().stream().map(
                                    HistoryTransition::fromStorageTransition
                            ).toList()
                    );

                AbortIncompleteMultipartUpload multipartUpload = lifecycleRule.getAbortIncompleteMultipartUpload();

                if(multipartUpload != null){
                    if(multipartUpload.getDaysAfterInitiation()>0){
                        ruleInput.setFragmentExpirationType(TransitionType.DAYS);
                        ruleInput.setFragmentExpirationDays(multipartUpload.getDaysAfterInitiation());
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

    private static void resolvePredicate(LifecycleFilterPredicate predicate, RuleInput ruleInput) {
        if(predicate instanceof LifecyclePrefixPredicate prefixPredicate){
            if(Utils.isNotBlank(prefixPredicate.getPrefix())){
                ruleInput.setEnablePrefix(true);
                ruleInput.setPrefix(prefixPredicate.getPrefix());
            }
        }else if(predicate instanceof LifecycleTagPredicate tagPredicate){
            if(tagPredicate.getTag() != null){
                ruleInput.setEnableTags(true);
                List<Tag> tags = ruleInput.getTags();
                if(tags == null){
                    tags = new ArrayList<>();
                    ruleInput.setTags(tags);
                }
                tags.add(new Tag(tagPredicate.getTag().getKey(), tagPredicate.getTag().getValue()));
            }
        }else if(predicate instanceof LifecycleAndOperator andOperator){
            if(Utils.isNotEmpty(andOperator.getOperands())){
                for (LifecycleFilterPredicate operand : andOperator.getOperands()) {
                    resolvePredicate(operand, ruleInput);
                }
            }
        }
    }
}
