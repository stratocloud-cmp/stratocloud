package com.stratocloud.resource;

import com.stratocloud.auth.RunWithSystemSession;
import com.stratocloud.utils.SelfMonitorTarget;
import com.stratocloud.utils.concurrent.ConcurrentUtil;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ResourceSyncScheduler implements SelfMonitorTarget {

    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(20);

    private final ResourceSynchronizer synchronizer;

    private static final Queue<SyncTask> syncTaskQueue = new ConcurrentLinkedQueue<>();

    public ResourceSyncScheduler(ResourceSynchronizer synchronizer) {
        this.synchronizer = synchronizer;
    }

    public static void addSyncTask(SyncTask syncTask){
        syncTaskQueue.offer(syncTask);
    }

    @Scheduled(fixedDelay = 15L, initialDelay = 30L, timeUnit = TimeUnit.SECONDS)
    @RunWithSystemSession
    public void scheduleAllTask() {
        for (int i = 0; i < syncTaskQueue.size(); i++) {
            SyncTask syncTask = syncTaskQueue.poll();

            if(syncTask == null)
                continue;

            if(syncTask.syncIntervalSeconds <= 0)
                continue;

            try {
                int syncTimes = Math.min(syncTask.syncTimes, 50);

                for (int j = 0; j < syncTimes; j++) {
                    Runnable runnable = () -> {
                        try {
                            synchronizer.synchronize(syncTask.resourceId);
                        } catch (Exception e) {
                            log.warn(e.toString());
                        }
                    };
                    executorService.schedule(
                            ConcurrentUtil.wrapWithContext(runnable),
                            syncTask.syncIntervalSeconds * (j+1),
                            TimeUnit.SECONDS
                    );
                }
            }catch (Exception e){
                log.warn(e.toString());
            }
        }
    }

    @Override
    public void logStats() {
        log.info("Resource synchronization scheduler executor service: {}", executorService);
    }

    public record SyncTask(Long resourceId,
                           long syncIntervalSeconds,
                           int syncTimes){
    }


    @PreDestroy
    public void onDestroy(){
        executorService.shutdownNow();
    }
}
