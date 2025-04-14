package com.stratocloud.job;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.utils.Utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class JobContext {

    private static final ThreadLocal<JobContext> threadLocal = new ThreadLocal<>();

    private final Long jobId;

    private final String jobType;

    private final Map<String, Object> runtimeProperties = new HashMap<>();

    public JobContext(Long jobId, String jobType, Map<String, Object> runtimeProperties) {
        this.jobId = jobId;
        this.jobType = jobType;

        if(Utils.isNotEmpty(runtimeProperties))
            this.runtimeProperties.putAll(runtimeProperties);
    }

    public static JobContext current(){
        JobContext jobContext = threadLocal.get();
        if(jobContext == null){
            throw new StratoException("Job context does not exist.");
        }
        return jobContext;
    }

    public static boolean exists(){
        return threadLocal.get() != null;
    }

    public static void create(Long jobId, String jobType) {
        create(jobId, jobType, new HashMap<>());
    }

    public static void create(Long jobId, String jobType, Map<String, Object> runtimeProperties) {
        JobContext jobContext = new JobContext(jobId, jobType, runtimeProperties);
        threadLocal.set(jobContext);
    }

    public Long getJobId() {
        return jobId;
    }

    public String getJobType() {
        return jobType;
    }

    public void addOutput(String key, Object value) {
        runtimeProperties.put(key, value);
    }

    public Map<String, Object> getRuntimeVariables() {
        return runtimeProperties;
    }

    public static final String KEY_RELATED_TAGS = "relatedTags";

    @SuppressWarnings("unchecked")
    public static void mergeRuntimeProperties(Map<String, Object> target, Map<String, Object> source){
        if(Utils.isEmpty(source))
            return;

        if(target == null)
            return;

        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if(entry.getValue() == null)
                continue;

            if(entry.getValue() instanceof Collection<?> sourceList &&
                    target.get(entry.getKey()) instanceof Collection<?> targetList){
                ((Collection<Object>) targetList).addAll(sourceList);
            } else {
                target.put(entry.getKey(), entry.getValue());
            }
        }
    }
}
