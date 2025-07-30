package com.stratocloud.provider.tencent.cos.lifecycle;

import com.qcloud.cos.model.AbortIncompleteMultipartUpload;
import com.qcloud.cos.model.BucketLifecycleConfiguration;
import com.qcloud.cos.model.Tag.LifecycleTagPredicate;
import com.qcloud.cos.model.lifecycle.LifecycleAndOperator;
import com.qcloud.cos.model.lifecycle.LifecycleFilter;
import com.qcloud.cos.model.lifecycle.LifecycleFilterPredicate;
import com.qcloud.cos.model.lifecycle.LifecyclePrefixPredicate;
import com.stratocloud.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public record TencentBucketLifecycleRule(TencentBucketLifecycleRuleId id,
                                         BucketLifecycleConfiguration.Rule detail) {

    public String getFilterDescription(){
        LifecycleFilter filter = detail.getFilter();

        String filterDescription;
        if(filter != null && filter.getPredicate() != null){
            LifecycleFilterPredicate predicate = filter.getPredicate();
            filterDescription = getPredicateDescription(predicate, true);
        }else {
            filterDescription = "整个存储桶";
        }

        return filterDescription;
    }

    public String getContentDescription(){
        List<String> contentLines = new ArrayList<>();

        List<BucketLifecycleConfiguration.Transition> transitions = detail.getTransitions();

        if(Utils.isNotEmpty(transitions)){
            for (BucketLifecycleConfiguration.Transition transition : transitions) {
                contentLines.add(getTransitionDescription(transition));
            }
        }

        if(detail.getExpirationInDays() > 0)
            contentLines.add("当前版本文件删除: %s天".formatted(detail.getExpirationInDays()));

        var historyVersionTransitions = detail.getNoncurrentVersionTransitions();

        if(Utils.isNotEmpty(historyVersionTransitions)){
            for (BucketLifecycleConfiguration.NoncurrentVersionTransition transition : historyVersionTransitions) {
                contentLines.add(getTransitionDescription(transition));
            }
        }

        if(detail.getNoncurrentVersionExpirationInDays() > 0)
            contentLines.add("历史版本文件删除: %s天".formatted(detail.getNoncurrentVersionExpirationInDays()));

        AbortIncompleteMultipartUpload multipartUpload = detail.getAbortIncompleteMultipartUpload();

        if(multipartUpload != null && multipartUpload.getDaysAfterInitiation() > 0)
            contentLines.add("碎片删除: %s天".formatted(multipartUpload.getDaysAfterInitiation()));

        return String.join("\n", contentLines);
    }

    private String getTransitionDescription(BucketLifecycleConfiguration.NoncurrentVersionTransition transition) {
        return getTransitionDescription("历史", transition.getStorageClassAsString(), transition.getDays());
    }

    private static String getTransitionDescription(BucketLifecycleConfiguration.Transition transition) {
        return getTransitionDescription("当前", transition.getStorageClass(), transition.getDays());
    }

    private static String getTransitionDescription(String prefix, String storageClass, int days){
        String suffix = switch (storageClass){
            case "STANDARD", "MAZ_STANDARD" -> "标准存储";
            case "STANDARD_IA", "MAZ_STANDARD_IA" -> "低频存储";
            case "INTELLIGENT_TIERING", "MAZ_INTELLIGENT_TIERING" -> "智能分层存储";
            case "ARCHIVE" -> "归档存储";
            case "DEEP_ARCHIVE" -> "深度归档存储";
            default -> storageClass;
        };

        return "%s版本文件沉降至%s: %s天".formatted(prefix, suffix, days);
    }

    private static String getPredicateDescription(LifecycleFilterPredicate predicate, boolean isTopLevel) {
        if(predicate instanceof LifecycleAndOperator andOperator){
            if(!isTopLevel)
                return "Unexpected multi-layered LifecycleAndOperator";


            if(Utils.isEmpty(andOperator.getOperands()))
                return "Unexpected empty LifecycleAndOperator";

            List<String> lines = new ArrayList<>();

            for (LifecycleFilterPredicate operand : andOperator.getOperands()) {
                lines.add(getPredicateDescription(operand, false));
            }

            return String.join("\n", lines);
        } else if(predicate instanceof LifecyclePrefixPredicate prefixPredicate){
            if(Utils.isBlank(prefixPredicate.getPrefix()))
                return "整个存储桶";

            return "前缀: %s".formatted(prefixPredicate.getPrefix());
        } else if(predicate instanceof LifecycleTagPredicate tagPredicate){
            if(tagPredicate.getTag() == null)
                return "整个存储桶";

            return "标签: [%s:%s]".formatted(tagPredicate.getTag().getKey(), tagPredicate.getTag().getValue());
        } else {
            return "Unexpected predicate type: %s".formatted(predicate.getClass().getSimpleName());
        }
    }


}
