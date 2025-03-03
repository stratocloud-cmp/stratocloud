package com.stratocloud.job;

import com.stratocloud.auth.CallContext;
import com.stratocloud.auth.RunWithSystemSession;
import com.stratocloud.job.cmd.CreateJobCmd;
import com.stratocloud.lock.DistributedLock;
import com.stratocloud.repository.ScheduledTriggerRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ScheduledTriggerWorker {

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(20);

    private final ScheduledTriggerRepository repository;

    private final JobService jobService;


    public ScheduledTriggerWorker(ScheduledTriggerRepository repository,
                                  JobService jobService) {
        this.repository = repository;
        this.jobService = jobService;
    }

    @DistributedLock(lockName = "SCHEDULED_TRIGGER_WORKER", maxLockSeconds = 60, waitIfLocked = false)
    @Scheduled(fixedDelay = 15L, initialDelay = 30L, timeUnit = TimeUnit.SECONDS)
    @RunWithSystemSession
    public void triggerNext(){
        List<ScheduledTrigger> triggers = repository.findByNextTriggerTimeBefore(LocalDateTime.now());

        for (ScheduledTrigger trigger : triggers) {
            try {
                triggerNext(trigger);
            }catch (Exception e){
                log.error("Scheduled trigger error: ", e);
            }
        }
    }

    public void triggerNext(ScheduledTrigger trigger){
        Optional<LocalDateTime> nextTriggerTime = trigger.generateNextTriggerTime();
        if(nextTriggerTime.isPresent()){
            trigger = repository.save(trigger);
            long delay = getDelay(nextTriggerTime.get());
            triggerByDelaySeconds(trigger, delay);
        }
    }

    public void triggerByDelaySeconds(ScheduledTrigger trigger, long delaySeconds) {
        Runnable runnable = () -> {
            CallContext.registerSystemSession();
            CreateJobCmd createJobCmd = jobService.createCmdForScheduledJob(trigger.getJobDefinition());
            jobService.createJob(createJobCmd);
            CallContext.unregister();
        };

        executorService.schedule(runnable, delaySeconds, TimeUnit.SECONDS);
    }

    private static long getDelay(LocalDateTime nextTriggerTime) {
        LocalDateTime now = LocalDateTime.now();
        ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(now);
        long next = nextTriggerTime.toEpochSecond(offset);
        long current = now.toEpochSecond(offset);
        return Math.max(0L, next - current);
    }

    @PreDestroy
    public void onDestroy(){
        executorService.shutdownNow();
    }
}
