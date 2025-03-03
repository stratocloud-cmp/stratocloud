package com.stratocloud.job;

import com.stratocloud.exceptions.StratoException;

public class AsyncJobContext {

    private static final ThreadLocal<AsyncJobContext> threadLocal = new ThreadLocal<>();

    private final AsyncJob asyncJob;

    public AsyncJobContext(JobContext jobContext) {
        Long jobId = jobContext.getJobId();
        asyncJob = new AsyncJob(jobId, jobContext.getJobType(), jobContext.getRuntimeVariables());
    }

    public AsyncJobContext(AsyncJob asyncJob) {
        this.asyncJob = asyncJob;
    }

    public static void create(){
        threadLocal.set(new AsyncJobContext(JobContext.current()));
    }

    public static void create(AsyncJob asyncJob){
        threadLocal.set(new AsyncJobContext(asyncJob));
    }

    static void destroy(){
        threadLocal.remove();
    }
    public static AsyncJobContext current(){
        AsyncJobContext asyncJobContext = threadLocal.get();
        if(asyncJobContext == null)
            throw new StratoException("AsyncJobContext does not exist.");
        return asyncJobContext;
    }




    public AsyncJob getAsyncJob() {
        return asyncJob;
    }
}
