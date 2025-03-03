package com.stratocloud.utils.concurrent;

import com.stratocloud.auth.CallContext;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.utils.SelfMonitorTarget;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


@Slf4j
@Component
public class ConcurrentUtil implements SelfMonitorTarget {

    private static final ExecutorService executorService = Executors.newFixedThreadPool(32);

    private static final ExecutorService secondaryExecutorService = Executors.newFixedThreadPool(32);

    public static void runAndWait(List<Runnable> tasks){
        runAndWaitAndGetErrors(tasks);
    }

    public static List<ExecutionException> runAndWaitAndGetErrors(List<Runnable> tasks) {
        List<ExecutionException> errors = new ArrayList<>();

        if(Utils.isEmpty(tasks))
            return errors;

        int tasksSize = tasks.size();

        ExecutorService selectedExecutorService = selectExecutorService(tasksSize);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Runnable task : tasks) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(wrapWithContext(task), selectedExecutorService);
            futures.add(future);
        }

        log.debug("{} runnable tasks submitted to the executor service.", tasksSize);



        for (CompletableFuture<Void> future : futures) {
            try {
                future.get();
            }catch (ExecutionException e){
                log.error("An error occurred during execution:", e.getCause());
                errors.add(e);
            } catch (InterruptedException e) {
                throw new StratoException("Task interrupted: ", e);
            }
        }

        log.debug("{} runnable tasks executed with {} errors.", tasksSize, errors.size());
        return errors;
    }


    private static Runnable wrapWithContext(Runnable runnable){
        if(!CallContext.exists()){
            return runnable;
        }

        final CallContext currentContext = CallContext.current();
        return () -> {
            CallContext.registerBack(currentContext);
            runnable.run();
            CallContext.unregister();
        };
    }

    private static ExecutorService selectExecutorService(int tasksSize){
        if(tasksSize < 500){
            return executorService;
        }else {
            log.warn("Too many tasks, using secondary executor service. TasksSize={}.", tasksSize);
            return secondaryExecutorService;
        }
    }

    @Override
    public void logStats() {
        log.info("Primary executor service: {}", executorService);
        log.info("Secondary executor service: {}", secondaryExecutorService);
    }
}
