package com.stratocloud.provider.aliyun.oss.actions;
import com.aliyun.oss.model.*;
import com.stratocloud.provider.aliyun.common.AliyunClient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.form.*;
import com.stratocloud.provider.aliyun.common.services.AliyunOssService;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.utils.TimeUtil;
import com.stratocloud.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Data
public class AliyunBucketUpdateLifecycleInput implements ResourceActionInput {
    @BooleanField(label = "开启访问跟踪")
    private boolean enableAccessMonitor;

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
                        "IA",
                        "Archive",
                        "ColdArchive",
                        "DeepColdArchive"
                },
                optionNames = {
                        "低频存储",
                        "归档存储",
                        "冷归档存储",
                        "深度冷归档存储"
                }
        )
        private StorageClass storageClass;
        @NumberField(label = "该天数后沉降", conditions = "this.type === 'DAYS'")
        private int days;
        @DateTimeField(label = "该日期后沉降", conditions = "this.type === 'DATE'", dateOnly = true)
        private LocalDateTime expirationDate;
        @BooleanField(label = "基于访问时间", conditions = "this.type === 'DAYS'")
        private boolean accessTime;
        @BooleanField(label = "对象再次访问后回归标准存储", conditions = "this.type === 'DAYS' && this.accessTime === true")
        private boolean returnToStdWhenVisit;

        @JsonIgnore
        public void validate() {
            if(isAccessTime() && storageClass != StorageClass.IA)
                throw new BadCommandException("基于访问时间仅支持低频存储");
        }

        public LifecycleRule.StorageTransition toStorageTransition() {
            if(type == TransitionType.DATE)
                return new LifecycleRule.StorageTransition(
                        TimeUtil.toDate(expirationDate),
                        storageClass
                );
            else
                return new LifecycleRule.StorageTransition(
                        days,
                        storageClass,
                        accessTime,
                        returnToStdWhenVisit
                );
        }

        public static Transition fromStorageTransition(LifecycleRule.StorageTransition storageTransition) {
            Integer expirationDays = storageTransition.getExpirationDays();
            Date createdBeforeDate = storageTransition.getCreatedBeforeDate();
            Boolean isAccessTime = storageTransition.getIsAccessTime();
            Boolean returnToStd = storageTransition.getReturnToStdWhenVisit();

            Transition transition = new Transition();

            if(expirationDays != null && expirationDays > 0) {
                transition.setType(TransitionType.DAYS);
                transition.setDays(expirationDays);
                transition.setStorageClass(storageTransition.getStorageClass());
                transition.setAccessTime(isAccessTime != null && isAccessTime);
                transition.setReturnToStdWhenVisit(returnToStd != null && returnToStd);
            } else if(createdBeforeDate != null) {
                transition.setType(TransitionType.DATE);
                transition.setStorageClass(storageTransition.getStorageClass());
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
                        "IA",
                        "Archive",
                        "ColdArchive",
                        "DeepColdArchive"
                },
                optionNames = {
                        "低频存储",
                        "归档存储",
                        "冷归档存储",
                        "深度冷归档存储"
                }
        )
        private StorageClass storageClass;
        @NumberField(label = "该天数后沉降")
        private int days;
        @BooleanField(label = "基于访问时间")
        private boolean accessTime;
        @BooleanField(label = "对象再次访问后回归标准存储", conditions = "this.accessTime === true")
        private boolean returnToStdWhenVisit;

        @JsonIgnore
        public void validate() {
            if(isAccessTime() && storageClass != StorageClass.IA)
                throw new BadCommandException("基于访问时间仅支持低频存储");
        }

        public LifecycleRule.NoncurrentVersionStorageTransition toStorageTransition() {
            return new LifecycleRule.NoncurrentVersionStorageTransition(
                    days,
                    storageClass,
                    accessTime,
                    returnToStdWhenVisit
            );
        }

        public static HistoryTransition fromStorageTransition(LifecycleRule.NoncurrentVersionStorageTransition transition) {
            HistoryTransition historyTransition = new HistoryTransition();
            historyTransition.setStorageClass(transition.getStorageClass());
            if(transition.getNoncurrentDays() != null)
                historyTransition.setDays(transition.getNoncurrentDays());
            if(transition.getIsAccessTime() != null)
                historyTransition.setAccessTime(transition.getIsAccessTime());
            if(transition.getReturnToStdWhenVisit() != null)
                historyTransition.setReturnToStdWhenVisit(transition.getReturnToStdWhenVisit());

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

        @BooleanField(label = "是否排除前缀")
        private boolean excludingPrefix;
        @InputField(label = "排除前缀")
        private String excludedPrefix;
        @BooleanField(label = "是否排除标签")
        private boolean excludingTags;
        @NestedFormField(label = "排除前缀", multiple = true, multipleMax = 1, nestedFormClass = Tag.class)
        private List<Tag> excludedTags;

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
                        "DATE",
                        "DISABLED"
                },
                optionNames = {
                        "按天数",
                        "按日期",
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
    public List<LifecycleRule> toLifecycleRules(){
        List<LifecycleRule> result = new ArrayList<>();

        if(!enabled || Utils.isEmpty(rules))
            return result;

        for (RuleInput ruleInput : rules) {
            LifecycleRule rule = new LifecycleRule();

            rule.setStatus(
                    enabled ? LifecycleRule.RuleStatus.Enabled : LifecycleRule.RuleStatus.Disabled
            );

            if(ruleInput.isEnablePrefix()){
                rule.setPrefix(ruleInput.getPrefix());
            }

            if(ruleInput.isEnableTags() && Utils.isNotEmpty(ruleInput.getTags())){
                rule.setTags(
                        ruleInput.getTags().stream().collect(
                                Collectors.toMap(Tag::getKey, Tag::getValue)
                        )
                );
            }

            if(ruleInput.isExcludingPrefix() || ruleInput.isExcludingTags()){
                LifecycleFilter filter = rule.getFilter();

                if(filter == null){
                    filter = new LifecycleFilter();
                    rule.setFilter(filter);
                }

                LifecycleNot lifecycleNot = new LifecycleNot();

                if(ruleInput.isExcludingPrefix())
                    lifecycleNot.setPrefix(ruleInput.getExcludedPrefix());

                if(ruleInput.isExcludingTags() && Utils.isNotEmpty(ruleInput.getExcludedTags())) {
                    Tag tag = ruleInput.getExcludedTags().get(0);
                    lifecycleNot.setTag(new com.aliyun.oss.model.Tag(tag.getKey(), tag.getValue()));
                }

                filter.setNotList(List.of(lifecycleNot));
            }

            if(ruleInput.getObjectSizeGreaterThan() != null || ruleInput.getObjectSizeLessThan() != null){
                LifecycleFilter filter = rule.getFilter();

                if(filter == null){
                    filter = new LifecycleFilter();
                    rule.setFilter(filter);
                }

                filter.setObjectSizeGreaterThan(ruleInput.getObjectSizeGreaterThan());
                filter.setObjectSizeLessThan(ruleInput.getObjectSizeLessThan());
            }

            switch (ruleInput.getExpirationType()){
                case DAYS -> rule.setExpirationDays(ruleInput.getExpirationDays());
                case DATE -> rule.setExpirationTime(TimeUtil.toDate(ruleInput.getExpirationDate()));
                case DELETE_MARKER -> rule.setExpiredDeleteMarker(true);
            }

            if(Utils.isNotEmpty(ruleInput.getTransitions()))
                rule.setStorageTransition(
                        ruleInput.getTransitions().stream().map(
                                Transition::toStorageTransition
                        ).toList()
                );

            if(ruleInput.getHistoryExpirationType() == TransitionType.DAYS){
                rule.setNoncurrentVersionExpiration(
                        new LifecycleRule.NoncurrentVersionExpiration(ruleInput.getHistoryExpirationDays())
                );
            }

            if(Utils.isNotEmpty(ruleInput.getHistoryTransitions()))
                rule.setNoncurrentVersionStorageTransitions(
                        ruleInput.getHistoryTransitions().stream().map(
                                HistoryTransition::toStorageTransition
                        ).toList()
                );

            if(ruleInput.getFragmentExpirationType() == TransitionType.DAYS){
                rule.setAbortMultipartUpload(
                        new LifecycleRule.AbortMultipartUpload(ruleInput.getFragmentExpirationDays())
                );
            } else if(ruleInput.getFragmentExpirationType() == TransitionType.DATE){
                rule.setAbortMultipartUpload(
                        new LifecycleRule.AbortMultipartUpload(
                                TimeUtil.toDate(ruleInput.getFragmentExpirationDate())
                        )
                );
            }

            result.add(rule);
        }

        return result;
    }

    public static AliyunBucketUpdateLifecycleInput getInput(AliyunClient client, String bucketName){
        AliyunBucketUpdateLifecycleInput input = new AliyunBucketUpdateLifecycleInput();

        AliyunOssService ossService = client.oss();
        Optional<AccessMonitor> accessMonitor = ossService.describeAccessMonitor(bucketName);

        input.setEnableAccessMonitor(
                accessMonitor.isPresent() && Objects.equals("Enabled", accessMonitor.get().getStatus())
        );

        List<LifecycleRule> lifecycleRules = ossService.describeBucketLifecycle(bucketName);

        if(Utils.isNotEmpty(lifecycleRules)){
            input.setEnabled(true);

            List<RuleInput> ruleInputs = new ArrayList<>();
            for (LifecycleRule lifecycleRule : lifecycleRules) {
                RuleInput ruleInput = new RuleInput();

                ruleInput.setEnabled(
                        LifecycleRule.RuleStatus.Enabled == lifecycleRule.getStatus()
                );

                if(Utils.isNotBlank(lifecycleRule.getPrefix())){
                    ruleInput.setEnablePrefix(true);
                    ruleInput.setPrefix(lifecycleRule.getPrefix());
                }

                if(Utils.isNotEmpty(lifecycleRule.getTags())){
                    Map<String, String> tags = lifecycleRule.getTags();

                    ruleInput.setEnableTags(true);
                    ruleInput.setTags(
                            tags.keySet().stream().map(
                                    k -> new Tag(
                                            k,
                                            tags.get(k)
                                    )
                            ).toList()
                    );
                }

                LifecycleFilter filter = lifecycleRule.getFilter();
                if(filter != null){
                    if(Utils.isNotEmpty(filter.getNotList())){
                        LifecycleNot lifecycleNot = filter.getNotList().get(0);

                        if(Utils.isNotBlank(lifecycleNot.getPrefix())){
                            ruleInput.setExcludingPrefix(true);
                            ruleInput.setExcludedPrefix(lifecycleNot.getPrefix());
                        }

                        if(lifecycleNot.getTag() != null){
                            ruleInput.setExcludingTags(true);
                            ruleInput.setExcludedTags(List.of(
                                    new Tag(
                                            lifecycleNot.getTag().getKey(),
                                            lifecycleNot.getTag().getValue()
                                    )
                            ));
                        }
                    }

                    ruleInput.setObjectSizeGreaterThan(filter.getObjectSizeGreaterThan());
                    ruleInput.setObjectSizeLessThan(filter.getObjectSizeLessThan());
                }

                int expirationDays = lifecycleRule.getExpirationDays();
                Date expirationTime = lifecycleRule.getExpirationTime();
                Boolean expiredDeleteMarker = lifecycleRule.getExpiredDeleteMarker();

                if(expirationDays > 0){
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

                if(Utils.isNotEmpty(lifecycleRule.getStorageTransition()))
                    ruleInput.setTransitions(
                            lifecycleRule.getStorageTransition().stream().map(
                                    Transition::fromStorageTransition
                            ).toList()
                    );

                var noncurrentVersionExpiration = lifecycleRule.getNoncurrentVersionExpiration();

                if(noncurrentVersionExpiration != null && noncurrentVersionExpiration.getNoncurrentDays() != null){
                    ruleInput.setHistoryExpirationType(TransitionType.DAYS);
                    ruleInput.setHistoryExpirationDays(noncurrentVersionExpiration.getNoncurrentDays());
                } else {
                    ruleInput.setHistoryExpirationType(TransitionType.DISABLED);
                }

                if(Utils.isNotEmpty(lifecycleRule.getNoncurrentVersionStorageTransitions()))
                    ruleInput.setHistoryTransitions(
                            lifecycleRule.getNoncurrentVersionStorageTransitions().stream().map(
                                    HistoryTransition::fromStorageTransition
                            ).toList()
                    );

                LifecycleRule.AbortMultipartUpload abortMultipartUpload = lifecycleRule.getAbortMultipartUpload();

                if(abortMultipartUpload != null){
                    if(abortMultipartUpload.getExpirationDays()>0){
                        ruleInput.setFragmentExpirationType(TransitionType.DAYS);
                        ruleInput.setFragmentExpirationDays(abortMultipartUpload.getExpirationDays());
                    }else if(abortMultipartUpload.getCreatedBeforeDate() != null){
                        ruleInput.setFragmentExpirationType(TransitionType.DATE);
                        ruleInput.setFragmentExpirationDate(
                                TimeUtil.fromDate(abortMultipartUpload.getCreatedBeforeDate())
                        );
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
