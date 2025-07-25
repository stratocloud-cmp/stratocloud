package com.stratocloud.provider.tencent.cos.lifecycle.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qcloud.cos.model.AbortIncompleteMultipartUpload;
import com.qcloud.cos.model.BucketLifecycleConfiguration;
import com.qcloud.cos.model.Tag.LifecycleTagPredicate;
import com.qcloud.cos.model.Tag.Tag;
import com.qcloud.cos.model.lifecycle.LifecycleAndOperator;
import com.qcloud.cos.model.lifecycle.LifecycleFilter;
import com.qcloud.cos.model.lifecycle.LifecycleFilterPredicate;
import com.qcloud.cos.model.lifecycle.LifecyclePrefixPredicate;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.utils.Utils;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
public class TencentBucketLifecycleRuleSpec {
    @BooleanField(label = "开启")
    private boolean enabled;

    @SelectField(
            label = "应用范围",
            options = {
                    "filtered",
                    "unfiltered"
            },
            optionNames = {
                    "指定范围",
                    "整个存储桶"
            },
            defaultValues = "filtered"
    )
    private String filterOption;

    @BooleanField(label = "指定对象前缀", defaultValue = true, conditions = "this.filterOption === 'filtered'")
    private boolean enablePrefix;

    @InputField(label = "对象前缀", conditions = "this.filterOption === 'filtered' && this.enablePrefix === true")
    private String prefix;

    @BooleanField(label = "指定对象标签", conditions = "this.filterOption === 'filtered'")
    private boolean enableTags;

    @SelectField(
            label = "对象标签",
            multiSelect = true,
            allowCreate = true,
            conditions = "this.filterOption === 'filtered' && this.enableTags === true",
            description = "标签键与标签值之间请使用:分隔"
    )
    private List<String> tags;

    @BooleanField(label = "管理当前版本文件", defaultValue = true)
    private boolean enableCvTransitions;
    @NumberField(
            label = "当前版本文件修改时间的N天后沉降至低频存储",
            conditions = "this.enableCvTransitions === true",
            placeHolder = "留空表示不沉降",
            required = false,
            min = 1,
            max = 3650
    )
    private Integer cvIaTransitionDays;
    @NumberField(
            label = "当前版本文件修改时间的N天后沉降至归档存储",
            conditions = "this.enableCvTransitions === true",
            placeHolder = "留空表示不沉降",
            required = false,
            min = 1,
            max = 3650
    )
    private Integer cvArchiveTransitionDays;
    @NumberField(
            label = "当前版本文件修改时间的N天后沉降至深度归档存储",
            conditions = "this.enableCvTransitions === true",
            placeHolder = "留空表示不沉降",
            required = false,
            min = 1,
            max = 3650
    )
    private Integer cvDeepArchiveTransitionDays;
    @NumberField(
            label = "当前版本文件修改时间的N天后删除",
            conditions = "this.enableCvTransitions === true",
            placeHolder = "留空表示不删除",
            required = false,
            min = 1,
            max = 3650
    )
    private Integer cvDeleteTransitionDays;


    @BooleanField(label = "管理历史版本文件")
    private boolean enableHvTransitions;
    @NumberField(
            label = "历史版本文件修改时间的N天后沉降至低频存储",
            conditions = "this.enableHvTransitions === true",
            placeHolder = "留空表示不沉降",
            required = false,
            min = 1,
            max = 3650
    )
    private Integer hvIaTransitionDays;
    @NumberField(
            label = "历史版本文件修改时间的N天后沉降至归档存储",
            conditions = "this.enableHvTransitions === true",
            placeHolder = "留空表示不沉降",
            required = false,
            min = 1,
            max = 3650
    )
    private Integer hvArchiveTransitionDays;
    @NumberField(
            label = "历史版本文件修改时间的N天后沉降至深度归档存储",
            conditions = "this.enableHvTransitions === true",
            placeHolder = "留空表示不沉降",
            required = false,
            min = 1,
            max = 3650
    )
    private Integer hvDeepArchiveTransitionDays;
    @NumberField(
            label = "历史版本文件修改时间的N天后删除",
            conditions = "this.enableHvTransitions === true",
            placeHolder = "留空表示不删除",
            required = false,
            min = 1,
            max = 3650
    )
    private Integer hvDeleteTransitionDays;


    @NumberField(
            label = "碎片创建的N天后删除",
            placeHolder = "留空表示不删除",
            required = false,
            min = 1,
            max = 3650
    )
    private Integer abortIncompleteDays;


    @JsonIgnore
    private List<Tag> validateAndGetTags() {
        List<Tag> result = new ArrayList<>();

        if(Utils.isEmpty(tags))
            return result;

        for (String tag : tags) {
            if(Utils.isBlank(tag) || !tag.contains(":"))
                throw new BadCommandException("对象标签格式有误");
            String[] split = tag.split(":");

            if(Utils.length(split) != 2)
                throw new BadCommandException("对象标签格式有误");

            String tagKey = split[0];
            String tagValue = split[1];

            if(Utils.isBlank(tagKey) || Utils.isBlank(tagValue))
                throw new BadCommandException("对象标签格式有误");

            result.add(new Tag(tagKey.trim(), tagValue.trim()));
        }

        return result;
    }

    @JsonIgnore
    public void validateAndApply(BucketLifecycleConfiguration.Rule rule, boolean isMultiAz){
        rule.setStatus(
                enabled ? BucketLifecycleConfiguration.ENABLED : BucketLifecycleConfiguration.DISABLED
        );

        if(Objects.equals(filterOption, "filtered")){
            List<LifecycleFilterPredicate> predicates = new ArrayList<>();

            if(enablePrefix){
                predicates.add(new LifecyclePrefixPredicate(prefix));
            }

            if(enableTags){
                for (Tag tag : validateAndGetTags()) {
                    predicates.add(new LifecycleTagPredicate(tag));
                }
            }

            if(predicates.isEmpty()){
                throw new BadCommandException("请至少指定一种应用范围");
            }else if(predicates.size() == 1){
                rule.setFilter(new LifecycleFilter(predicates.get(0)));
            }else {
                rule.setFilter(new LifecycleFilter(new LifecycleAndOperator(predicates)));
            }
        }else {
            rule.setFilter(null);
        }

        if(!enableCvTransitions && !enableHvTransitions && abortIncompleteDays == null)
            throw new BadCommandException("请至少指定一项规则");

        if(enableCvTransitions){
            List<BucketLifecycleConfiguration.Transition> transitions = getTransitions(isMultiAz);

            if(cvDeleteTransitionDays != null){
                rule.setExpirationInDays(cvDeleteTransitionDays);
            }

            if(transitions.isEmpty() && rule.getExpirationInDays()<=0)
                throw new BadCommandException("请至少指定一项当前版本文件规则");

            rule.setTransitions(transitions);
        }

        if(enableHvTransitions){
            var transitions = getHvTransitions(isMultiAz);

            if(hvDeleteTransitionDays != null){
                rule.setNoncurrentVersionExpirationInDays(hvDeleteTransitionDays);
            }

            if(transitions.isEmpty() && rule.getNoncurrentVersionExpirationInDays()<=0)
                throw new BadCommandException("请至少指定一项历史版本规则");

            rule.setNoncurrentVersionTransitions(transitions);
        }

        if(abortIncompleteDays != null){
            rule.setAbortIncompleteMultipartUpload(
                    new AbortIncompleteMultipartUpload().withDaysAfterInitiation(abortIncompleteDays)
            );
        }
    }

    @JsonIgnore
    private List<BucketLifecycleConfiguration.Transition> getTransitions(boolean isMultiAz) {
        List<BucketLifecycleConfiguration.Transition> transitions = new ArrayList<>();

        if(cvIaTransitionDays != null){
            var transition = new BucketLifecycleConfiguration.Transition();
            transition.setDays(cvIaTransitionDays);
            transition.setStorageClass(
                    isMultiAz ? "MAZ_STANDARD_IA" : "STANDARD_IA"
            );
            transitions.add(transition);
        }

        if(cvArchiveTransitionDays != null){
            var transition = new BucketLifecycleConfiguration.Transition();
            transition.setDays(cvArchiveTransitionDays);
            transition.setStorageClass("ARCHIVE");
            transitions.add(transition);
        }

        if(cvDeepArchiveTransitionDays != null){
            var transition = new BucketLifecycleConfiguration.Transition();
            transition.setDays(cvDeepArchiveTransitionDays);
            transition.setStorageClass("DEEP_ARCHIVE");
            transitions.add(transition);
        }
        return transitions;
    }

    @JsonIgnore
    private List<BucketLifecycleConfiguration.NoncurrentVersionTransition> getHvTransitions(boolean isMultiAz) {
        List<BucketLifecycleConfiguration.NoncurrentVersionTransition> transitions = new ArrayList<>();

        if(hvIaTransitionDays != null){
            var transition = new BucketLifecycleConfiguration.NoncurrentVersionTransition();
            transition.setDays(hvIaTransitionDays);
            transition.setStorageClass(
                    isMultiAz ? "MAZ_STANDARD_IA" : "STANDARD_IA"
            );
            transitions.add(transition);
        }

        if(hvArchiveTransitionDays != null){
            var transition = new BucketLifecycleConfiguration.NoncurrentVersionTransition();
            transition.setDays(hvArchiveTransitionDays);
            transition.setStorageClass("ARCHIVE");
            transitions.add(transition);
        }

        if(hvDeepArchiveTransitionDays != null){
            var transition = new BucketLifecycleConfiguration.NoncurrentVersionTransition();
            transition.setDays(hvDeepArchiveTransitionDays);
            transition.setStorageClass("DEEP_ARCHIVE");
            transitions.add(transition);
        }
        return transitions;
    }
}
