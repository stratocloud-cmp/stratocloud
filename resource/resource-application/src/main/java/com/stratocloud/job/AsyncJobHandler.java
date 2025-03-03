package com.stratocloud.job;

import com.stratocloud.request.JobParameters;
import com.stratocloud.utils.Utils;

import java.util.ArrayList;
import java.util.List;


public interface AsyncJobHandler<P extends JobParameters> extends JobHandler<P>{
    /**
     * Transient job will not keep executions data after the job ends.
     * @return whether the job is transient.
     */
    default boolean isTransientJob(){
        return false;
    }

    default void onJobEnded(AsyncJob asyncJob){}

    @Override
    default List<String> collectSummaryData(P jobParameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        List<Task> tasks = asyncJob.getTasks();

        List<String> result = new ArrayList<>();

        if(Utils.isEmpty(tasks))
            return result;

        for (Task task : tasks) {
            result.add("%s(%s)".formatted(task.getName(), task.getEntityDescription()));
        }


        return result;
    }
}
